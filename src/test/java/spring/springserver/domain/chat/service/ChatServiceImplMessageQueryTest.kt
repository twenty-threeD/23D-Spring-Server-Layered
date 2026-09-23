package spring.springserver.domain.chat.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Pageable
import org.springframework.data.redis.core.ListOperations
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.transaction.PlatformTransactionManager
import spring.springserver.domain.chat.data.response.ChatMessageResponse
import spring.springserver.domain.chat.data.response.ChatMessageType
import spring.springserver.domain.chat.entity.ChatMessage
import spring.springserver.domain.chat.entity.ChatRoom
import spring.springserver.domain.chat.entity.ChatRoomParticipant
import spring.springserver.domain.chat.repository.ChatMessageRepository
import spring.springserver.domain.chat.repository.ChatRoomParticipantRepository
import spring.springserver.domain.chat.repository.ChatRoomRepository
import spring.springserver.domain.chat.service.impl.ChatServiceImpl
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.domain.member.service.MemberService
import spring.springserver.domain.post.repository.PostRepository
import spring.springserver.domain.profile.service.ProfileService
import java.time.Instant

/**
 * 메시지 조회가 Redis 캐시와 PostgreSQL 사이에서 어느 쪽을 읽는지 검증한다.
 *
 * 캐시가 사라져도 DB가 진실 원본으로 동작해야 하고, 캐시로 충분할 때는 DB를 건드리지 않아야 한다.
 */
class ChatServiceImplMessageQueryTest {

    private val chatRoomRepository = mock<ChatRoomRepository>()
    private val chatRoomParticipantRepository = mock<ChatRoomParticipantRepository>()
    private val chatMessageRepository = mock<ChatMessageRepository>()
    private val memberRepository = mock<MemberRepository>()
    private val postRepository = mock<PostRepository>()
    private val profileService = mock<ProfileService>()
    private val memberService = mock<MemberService>()
    private val redisTemplate = mock<RedisTemplate<String, String>>()
    private val listOperations = mock<ListOperations<String, String>>()
    private val messagingTemplate = mock<SimpMessagingTemplate>()
    private val transactionManager = mock<PlatformTransactionManager>()

    private val objectMapper: ObjectMapper = Jackson2ObjectMapperBuilder.json().build()

    private lateinit var chatService: ChatServiceImpl

    companion object {

        private const val ROOM_ID = 1L
        private const val USERNAME = "tester"
        private const val CACHE_KEY = "chat:room:$ROOM_ID:messages:v2"

        /**
         * 구현이 캐시를 뒤에서부터 훑으므로(`ChatServiceImpl.chatMessageCacheScanSize`)
         * 스텁도 같은 구간으로 맞춘다.
         */
        private const val CACHE_SCAN_START = -200L
    }

    @BeforeEach
    fun setUp() {

        chatService = ChatServiceImpl(
            chatRoomRepository = chatRoomRepository,
            chatRoomParticipantRepository = chatRoomParticipantRepository,
            chatMessageRepository = chatMessageRepository,
            memberRepository = memberRepository,
            postRepository = postRepository,
            profileService = profileService,
            memberService = memberService,
            redisTemplate = redisTemplate,
            objectMapper = objectMapper,
            transactionManager = transactionManager,
            messagingTemplate = messagingTemplate
        )

        whenever(redisTemplate.opsForList()).thenReturn(listOperations)

        givenParticipant(visibleParticipant())
    }

    @Test
    @DisplayName("캐시가 요청한 개수를 채우면 DB를 조회하지 않고 오래된 순으로 돌려준다")
    fun readsFromCacheWhenCacheIsEnough() {

        givenCachedMessages(1L, 2L, 3L)

        val messages = getMessages(size = 3)

        assertThat(messages.map { it.messageId }).containsExactly(1L, 2L, 3L)
        verify(chatMessageRepository, never()).findRecentByRoomId(any(), any(), any())
        verify(chatMessageRepository, never()).findByRoomIdBefore(any(), any(), any(), any())
    }

    @Test
    @DisplayName("캐시가 비면 DB 최신 페이지를 읽는다")
    fun fallsBackToDatabaseWhenCacheIsEmpty() {

        givenCachedMessages()

        val storedMessages = listOf(storedMessage(20L), storedMessage(19L))

        whenever(
            chatMessageRepository.findRecentByRoomId(
                eq(ROOM_ID),
                eq(Instant.EPOCH),
                any()
            )
        ).thenReturn(storedMessages)

        val messages = getMessages(size = 50)

        assertThat(messages.map { it.messageId }).containsExactly(19L, 20L)
    }

    @Test
    @DisplayName("캐시가 부족하면 캐시에서 가장 오래된 id를 커서로 DB를 이어 읽는다")
    fun mergesCacheAndDatabase() {

        givenCachedMessages(10L, 11L)

        val storedMessages = listOf(storedMessage(9L))

        whenever(
            chatMessageRepository.findByRoomIdBefore(
                eq(ROOM_ID),
                eq(10L),
                eq(Instant.EPOCH),
                any()
            )
        ).thenReturn(storedMessages)

        val messages = getMessages(size = 3)

        assertThat(messages.map { it.messageId }).containsExactly(9L, 10L, 11L)
    }

    @Test
    @DisplayName("커서를 주면 그보다 작은 id만 키셋으로 읽는다")
    fun readsOlderPageByCursor() {

        givenCachedMessages(10L, 11L)

        val storedMessages = listOf(storedMessage(4L), storedMessage(3L))

        whenever(
            chatMessageRepository.findByRoomIdBefore(
                eq(ROOM_ID),
                eq(5L),
                eq(Instant.EPOCH),
                any()
            )
        ).thenReturn(storedMessages)

        val messages = getMessages(cursor = 5L, size = 2)

        assertThat(messages.map { it.messageId }).containsExactly(3L, 4L)
    }

    @Test
    @DisplayName("after가 캐시 구간 안이면 DB 없이 델타만 돌려준다")
    fun syncsDeltaFromCache() {

        givenCachedMessages(7L, 8L, 9L)

        val messages = getMessages(after = 7L, size = 50)

        assertThat(messages.map { it.messageId }).containsExactly(8L, 9L)
        verify(chatMessageRepository, never()).findByRoomIdAfter(any(), any(), any(), any())
    }

    @Test
    @DisplayName("after가 캐시보다 오래된 지점이면 DB에서 델타를 읽는다")
    fun syncsDeltaFromDatabaseWhenCacheStartsLater() {

        givenCachedMessages(7L, 8L)

        val storedMessages = listOf(storedMessage(3L), storedMessage(4L))

        whenever(
            chatMessageRepository.findByRoomIdAfter(
                eq(ROOM_ID),
                eq(2L),
                eq(Instant.EPOCH),
                any()
            )
        ).thenReturn(storedMessages)

        val messages = getMessages(after = 2L, size = 50)

        assertThat(messages.map { it.messageId }).containsExactly(3L, 4L)
    }

    @Test
    @DisplayName("size는 상한 100으로 잘린다")
    fun clampsPageSize() {

        givenCachedMessages()

        whenever(chatMessageRepository.findRecentByRoomId(eq(ROOM_ID), eq(Instant.EPOCH), any()))
            .thenReturn(emptyList())

        getMessages(size = 500)

        val pageableCaptor = org.mockito.kotlin.argumentCaptor<Pageable>()

        verify(chatMessageRepository).findRecentByRoomId(eq(ROOM_ID), eq(Instant.EPOCH), pageableCaptor.capture())

        assertThat(pageableCaptor.firstValue.pageSize).isEqualTo(100)
    }

    @Test
    @DisplayName("나간 시점 이전 메시지는 캐시에서도 제외된다")
    fun hidesMessagesBeforeLeaveWatermark() {

        val leftAt = Instant.parse("2026-01-01T00:00:00Z")

        givenParticipant(visibleParticipant(deletedAt = leftAt))

        givenCachedRawMessages(
            cachedMessage(1L, leftAt.minusSeconds(10)),
            cachedMessage(2L, leftAt.plusSeconds(10))
        )

        whenever(chatMessageRepository.findByRoomIdBefore(eq(ROOM_ID), eq(2L), eq(leftAt), any()))
            .thenReturn(emptyList())

        val messages = getMessages(size = 50)

        assertThat(messages.map { it.messageId }).containsExactly(2L)
    }

    private fun getMessages(
        cursor: Long? = null,
        after: Long? = null,
        size: Int
    ): List<ChatMessageResponse> =
        chatService.getRoomMessages(
            username = USERNAME,
            roomId = ROOM_ID,
            cursor = cursor,
            after = after,
            size = size
        )

    /**
     * 참여자 스텁을 먼저 완성한 뒤 리포지토리에 물린다.
     * `thenReturn` 안에서 다른 목을 스텁하면 Mockito가 미완성 스텁으로 본다.
     */
    private fun givenParticipant(
        participant: ChatRoomParticipant
    ) {

        whenever(chatRoomParticipantRepository.findByRoomIdAndMemberUsername(ROOM_ID, USERNAME))
            .thenReturn(participant)
    }

    private fun visibleParticipant(
        deletedAt: Instant? = null
    ): ChatRoomParticipant {

        val participant = mock<ChatRoomParticipant>()

        whenever(participant.visible).thenReturn(true)
        whenever(participant.deletedAt).thenReturn(deletedAt)

        return participant
    }

    private fun givenCachedMessages(
        vararg messageIds: Long
    ) {

        givenCachedRawMessages(
            *messageIds.map { cachedMessage(it, Instant.parse("2026-02-01T00:00:00Z").plusSeconds(it)) }
                .toTypedArray()
        )
    }

    private fun givenCachedRawMessages(
        vararg messages: ChatMessageResponse
    ) {

        whenever(listOperations.range(CACHE_KEY, CACHE_SCAN_START, -1))
            .thenReturn(messages.map { objectMapper.writeValueAsString(it) })
    }

    private fun cachedMessage(
        messageId: Long,
        createdAt: Instant
    ): ChatMessageResponse =
        ChatMessageResponse(
            messageId = messageId,
            roomId = ROOM_ID,
            senderUsername = USERNAME,
            senderName = "테스터",
            message = "cached-$messageId",
            createdAt = createdAt,
            attachedFileUrls = emptyList(),
            type = ChatMessageType.TEXT
        )

    /**
     * DB에서 읽어온 행. 엔티티는 JPA가 채우는 값이 많아 스텁으로 세운다.
     */
    private fun storedMessage(
        messageId: Long
    ): ChatMessage {

        val room = mock<ChatRoom>()
        val sender = mock<Member>()
        val chatMessage = mock<ChatMessage>()

        whenever(room.getId()).thenReturn(ROOM_ID)
        whenever(sender.username).thenReturn(USERNAME)
        whenever(sender.name).thenReturn("테스터")
        whenever(chatMessage.getId()).thenReturn(messageId)
        whenever(chatMessage.room).thenReturn(room)
        whenever(chatMessage.sender).thenReturn(sender)
        whenever(chatMessage.message).thenReturn("stored-$messageId")
        whenever(chatMessage.createdAt).thenReturn(Instant.parse("2026-01-15T00:00:00Z"))
        whenever(chatMessage.messageType).thenReturn(ChatMessageType.TEXT)
        whenever(chatMessage.attachmentUrls()).thenReturn(emptyList())
        whenever(chatMessage.paymentPayload).thenReturn(null)

        return chatMessage
    }
}
