package spring.springserver.domain.chat.service.impl

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import spring.springserver.domain.chat.data.request.CreateChatRoomRequest
import spring.springserver.domain.chat.data.request.SendChatMessageRequest
import spring.springserver.domain.chat.data.response.ChatMessageResponse
import spring.springserver.domain.chat.data.response.ChatMessageType
import spring.springserver.domain.chat.data.response.ChatParticipantResponse
import spring.springserver.domain.chat.data.response.ChatPaymentResponse
import spring.springserver.domain.chat.data.response.ChatRoomResponse
import spring.springserver.domain.chat.data.response.CreateChatRoomResponse
import spring.springserver.domain.chat.entity.ChatMessage
import spring.springserver.domain.chat.entity.ChatRoom
import spring.springserver.domain.chat.entity.ChatRoomParticipant
import spring.springserver.domain.chat.repository.ChatMessageRepository
import spring.springserver.domain.chat.repository.ChatRoomParticipantRepository
import spring.springserver.domain.chat.repository.ChatRoomRepository
import spring.springserver.domain.chat.service.ChatService
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.domain.member.service.MemberService
import spring.springserver.domain.post.entity.Post
import spring.springserver.domain.post.exception.PostStatusCode
import spring.springserver.domain.post.repository.PostRepository
import spring.springserver.domain.profile.service.ProfileService
import spring.springserver.global.exception.exception.ApplicationException
import spring.springserver.global.exception.status_code.CommonStatusCode
import java.time.Instant
import java.util.concurrent.TimeUnit

@Service
@Transactional
class ChatServiceImpl(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatRoomParticipantRepository: ChatRoomParticipantRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val memberRepository: MemberRepository,
    private val postRepository: PostRepository,
    private val profileService: ProfileService,
    private val memberService: MemberService,
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    transactionManager: PlatformTransactionManager,
    private val messagingTemplate: SimpMessagingTemplate
) : ChatService {

    private val chatMessageCacheTtlMillis = TimeUnit.DAYS.toMillis(3)
    private val chatMessageCacheScanSize = 200L
    private val paymentMessagePreview = "결제 완료"

    private val createRoomTransactionTemplate = TransactionTemplate(transactionManager).apply {

        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    override fun createOrGetDirectRoom(
        requesterUsername: String,
        createChatRoomRequest: CreateChatRoomRequest
    ): CreateChatRoomResponse {

        memberService.ensurePhoneVerified(username = requesterUsername)

        if (requesterUsername == createChatRoomRequest.username) {

            throw ApplicationException.of(
                CommonStatusCode.INVALID_ARGUMENT,
                "자기 자신과는 채팅방을 만들 수 없습니다."
            )
        }

        return try {

            createRoomTransactionTemplate.execute {

                createOrGetDirectRoomInTransaction(
                    requesterUsername = requesterUsername,
                    targetUsername = createChatRoomRequest.username,
                    postId = createChatRoomRequest.postId
                )
            }!!
        } catch (_: DataIntegrityViolationException) {

            createRoomTransactionTemplate.execute {

                getExistingDirectRoomResponse(
                    requesterUsername = requesterUsername,
                    targetUsername = createChatRoomRequest.username,
                    postId = createChatRoomRequest.postId
                )
            }!!
        }
    }

    private fun createOrGetDirectRoomInTransaction(
        requesterUsername: String,
        targetUsername: String,
        postId: Long?
    ): CreateChatRoomResponse {

        val requester = getMemberByUsername(requesterUsername)
        val target = getMemberByUsername(targetUsername)
        val post = resolvePost(postId)
        val directChatKey = ChatRoom.generateDirectChatKey(requester.getId()!!, target.getId()!!)

        val existingRoom = chatRoomRepository.findByDirectChatKeyAndPostId(
            directChatKey = directChatKey,
            postId = post.getId()!!
        )

        if (existingRoom != null) {

            ensureParticipantRows(existingRoom)

            val participant = reactivateParticipant(existingRoom, requester)

            return CreateChatRoomResponse.of(
                roomId = existingRoom.getId(),
                postId = existingRoom.post.getId(),
                participantUsername = target.username,
                existingRoom = true,
                clearBefore = participant.deletedAt
            )
        }

        return createRoom(
            requester = requester,
            target = target,
            post = post
        )
    }

    override fun getMyChatRooms(
        username: String
    ): List<ChatRoomResponse> {

        /**
         * 백필은 참여자 테이블 도입 전 방을 위한 호환 장치다. 목록 조회마다 돌리면
         * 읽기 요청이 매번 쓰기 트랜잭션이 되므로, 빠진 방이 실제로 있을 때만 보정한다.
         *
         * 조회 결과가 비었는지로 판단하면 안 된다. 신규 방과 과거 방을 함께 가진 회원은
         * 결과가 비지 않으므로 보정이 영영 돌지 않고 과거 방이 계속 누락된다.
         */
        if (chatRoomRepository.countRoomsMissingParticipantRow(username) > 0) {

            backfillParticipantRowsForMember(username)
        }

        val myParticipants = chatRoomParticipantRepository.findVisibleParticipantsByUsername(username)

        if (myParticipants.isEmpty()) {

            return emptyList()
        }

        val othersByRoomId = chatRoomParticipantRepository
            .findAllByRoomIds(myParticipants.mapNotNull { it.room.getId() })
            .filter { it.member.username != username }
            .associateBy({ it.room.getId() }, { it.member })

        val imageUrls = profileService.getImageUrlsByMemberIds(
            othersByRoomId.values.mapNotNull { it.getId() }
        )

        return myParticipants.mapNotNull { participant ->

            val room = participant.room
            val other = othersByRoomId[room.getId()]
                ?: return@mapNotNull null

            ChatRoomResponse.of(
                room = room,
                participant = other,
                participantImageUrl = other.getId()?.let { imageUrls[it] },
                clearBefore = participant.deletedAt
            )
        }
    }

    /**
     * 최근 메시지는 Redis 캐시에서, 캐시에 없거나 더 오래된 구간은 PostgreSQL에서 읽는다.
     *
     * 페이지네이션은 offset이 아니라 커서(`id < :cursor`)다. 반환은 기존 계약대로 오래된 -> 최신 순이다.
     * `after`가 오면 과거 스크롤이 아니라 델타 동기화이므로 그쪽으로 넘긴다(둘은 배타적이다).
     */
    @Transactional(readOnly = true)
    override fun getRoomMessages(
        username: String,
        roomId: Long,
        cursor: Long?,
        after: Long?,
        size: Int
    ): List<ChatMessageResponse> {

        val participant = getVisibleParticipant(
            roomId = roomId,
            username = username
        )

        val pageSize = normalizePageSize(size)
        val clearBefore = participant.deletedAt
        val createdAfter = clearBefore ?: Instant.EPOCH

        if (after != null) {

            return getMessagesAfter(
                roomId = roomId,
                sinceId = after,
                clearBefore = clearBefore,
                createdAfter = createdAfter,
                pageSize = pageSize
            )
        }

        val cachedMessages = getCachedRoomMessages(roomId)
            .filter { clearBefore == null || it.createdAt.isAfter(clearBefore) }
            .filter { it.messageId != null && (cursor == null || it.messageId < cursor) }
            .sortedByDescending { it.messageId }
            .take(pageSize)

        if (cachedMessages.size >= pageSize) {

            return cachedMessages.asReversed()
        }

        val nextCursor = cachedMessages.lastOrNull()?.messageId
            ?: cursor
        val remaining = pageSize - cachedMessages.size
        val pageable = PageRequest.of(0, remaining)

        val storedMessages = if (nextCursor == null) {

            chatMessageRepository.findRecentByRoomId(
                roomId = roomId,
                clearBefore = createdAfter,
                pageable = pageable
            )
        } else {

            chatMessageRepository.findByRoomIdBefore(
                roomId = roomId,
                lastId = nextCursor,
                clearBefore = createdAfter,
                pageable = pageable
            )
        }

        return (cachedMessages + storedMessages.map { toResponse(it) }).asReversed()
    }

    /**
     * 클라이언트가 아는 마지막 메시지 이후만 돌려준다.
     *
     * Redis 리스트는 TTL 안의 메시지를 빠짐없이 순서대로 들고 있으므로,
     * 요청한 지점이 캐시의 시작보다 뒤라면 그 이후는 전부 캐시에 있다고 볼 수 있어 DB를 건드리지 않는다.
     * 그보다 오래된 지점이면(오래 접속하지 않은 클라이언트) DB에서 읽는다.
     *
     * 반환 개수가 `pageSize`와 같으면 아직 남은 구간이 있다는 뜻이고,
     * 클라이언트는 마지막 id로 `after`를 갱신해 다시 호출하면 된다.
     */
    private fun getMessagesAfter(
        roomId: Long,
        sinceId: Long,
        clearBefore: Instant?,
        createdAfter: Instant,
        pageSize: Int
    ): List<ChatMessageResponse> {

        val cachedMessages = getCachedRoomMessages(roomId)
            .filter { clearBefore == null || it.createdAt.isAfter(clearBefore) }
            .filter { it.messageId != null }
            .sortedBy { it.messageId }

        val oldestCachedId = cachedMessages.firstOrNull()?.messageId

        if (oldestCachedId != null && sinceId + 1 >= oldestCachedId) {

            return cachedMessages.filter { it.messageId!! > sinceId }
                .take(pageSize)
        }

        return chatMessageRepository.findByRoomIdAfter(
            roomId = roomId,
            sinceId = sinceId,
            clearBefore = createdAfter,
            pageable = PageRequest.of(0, pageSize)
        )
            .map { toResponse(it) }
    }

    override fun sendMessage(
        senderUsername: String,
        sendChatMessageRequest: SendChatMessageRequest
    ): ChatMessageResponse {

        val senderParticipant = getVisibleParticipant(
            roomId = sendChatMessageRequest.roomId,
            username = senderUsername
        )
        val room = senderParticipant.room
        val sender = senderParticipant.member

        memberService.ensurePhoneVerified(member = sender)
        val normalizedMessage = normalizeMessage(sendChatMessageRequest.message)
        val attachmentUrls = normalizeAttachmentUrls(sendChatMessageRequest.fileUrls)

        if (normalizedMessage.isBlank() && attachmentUrls.isEmpty()) {

            throw ApplicationException.of(
                CommonStatusCode.INVALID_ARGUMENT,
                "메시지나 첨부 파일 중 하나는 필요합니다."
            )
        }

        val createdAt = Instant.now()

        val chatMessage = chatMessageRepository.save(
            ChatMessage(
                room = room,
                sender = sender,
                message = normalizedMessage,
                createdAt = createdAt,
                messageType = ChatMessageType.TEXT,
                attachmentUrlsText = ChatMessage.joinAttachmentUrls(attachmentUrls)
            )
        )

        val response = ChatMessageResponse(
            messageId = chatMessage.getId(),
            roomId = room.getId(),
            senderUsername = sender.username,
            senderName = sender.name,
            message = normalizedMessage,
            createdAt = createdAt,
            attachedFileUrls = attachmentUrls
        )

        room.updateLastMessageMeta(
            at = createdAt,
            preview = "새 메시지"
        )

        reactivateParticipantsOnNewMessage(
            room = room,
            senderId = sender.getId()
        )

        appendRoomMessage(room.getId()!!, response)

        return response
    }

    override fun getOtherParticipantUsername(
        roomId: Long,
        username: String
    ): ChatParticipantResponse {

        getVisibleParticipant(roomId, username)

        return ChatParticipantResponse(getOtherMember(roomId, username).username)
    }

    override fun canAccessRoom(
        username: String,
        roomId: Long
    ): Boolean {

        val participant = findParticipant(
            roomId = roomId,
            username = username
        )
            ?: return false

        return participant.visible
    }

    override fun leaveRoom(
        username: String,
        roomId: Long
    ) {

        val participant = getVisibleParticipant(roomId, username)
        participant.leave(Instant.now())
    }

    override fun findDirectRoomId(
        clientId: Long,
        professionalId: Long,
        postId: Long
    ): Long? {

        return chatRoomRepository.findByDirectChatKeyAndPostId(
            directChatKey = ChatRoom.generateDirectChatKey(clientId, professionalId),
            postId = postId
        )?.getId()
    }

    override fun isRoomParticipant(
        roomId: Long,
        memberId: Long
    ): Boolean {

        if (chatRoomParticipantRepository.existsByRoomIdAndMemberId(roomId, memberId)) {

            return true
        }

        backfillParticipantRows(roomId)

        return chatRoomParticipantRepository.existsByRoomIdAndMemberId(roomId, memberId)
    }

    override fun sendPaymentMessage(
        roomId: Long,
        senderMemberId: Long,
        payment: ChatPaymentResponse
    ): ChatMessageResponse {

        val senderParticipant = findParticipant(
            roomId = roomId,
            memberId = senderMemberId
        )
            ?: throw ApplicationException.of(CommonStatusCode.INVALID_ARGUMENT)

        val room = senderParticipant.room
        val sender = senderParticipant.member
        val createdAt = Instant.now()

        val chatMessage = chatMessageRepository.save(
            ChatMessage(
                room = room,
                sender = sender,
                message = paymentMessagePreview,
                createdAt = createdAt,
                messageType = ChatMessageType.PAYMENT,
                paymentPayload = objectMapper.writeValueAsString(payment)
            )
        )

        val response = ChatMessageResponse(
            messageId = chatMessage.getId(),
            roomId = roomId,
            senderUsername = sender.username,
            senderName = sender.name,
            message = paymentMessagePreview,
            createdAt = createdAt,
            attachedFileUrls = emptyList(),
            type = ChatMessageType.PAYMENT,
            payment = payment
        )

        room.updateLastMessageMeta(
            at = createdAt,
            preview = paymentMessagePreview
        )

        reactivateParticipantsOnNewMessage(
            room = room,
            senderId = sender.getId()
        )

        appendRoomMessage(
            roomId = roomId,
            message = response
        )

        messagingTemplate.convertAndSend(
            "/topic/chat/rooms/$roomId",
            response
        )

        return response
    }

    /**
     * 1:1 방만 만든다. 참여자를 두 명으로 제한하는 책임은 스키마가 아니라 이 서비스 계층에 있다.
     * 방과 참여자 row는 같은 트랜잭션에서 저장해 참여자 없는 방이 남지 않게 한다.
     */
    private fun createRoom(
        requester: Member,
        target: Member,
        post: Post
    ): CreateChatRoomResponse {

        val room = if (isProfessional(requester)) {

            ChatRoom(client = target, professional = requester, post = post)
        } else {

            ChatRoom(client = requester, professional = target, post = post)
        }

        val savedRoom = chatRoomRepository.saveAndFlush(room)

        chatRoomParticipantRepository.saveAll(
            listOf(
                ChatRoomParticipant(
                    room = savedRoom,
                    member = requester
                ),
                ChatRoomParticipant(
                    room = savedRoom,
                    member = target
                )
            )
        )

        return CreateChatRoomResponse.of(
            roomId = savedRoom.getId(),
            postId = savedRoom.post.getId(),
            participantUsername = target.username,
            existingRoom = false,
            clearBefore = null
        )
    }

    private fun getExistingDirectRoomResponse(
        requesterUsername: String,
        targetUsername: String,
        postId: Long?
    ): CreateChatRoomResponse {

        val requester = getMemberByUsername(requesterUsername)
        val target = getMemberByUsername(targetUsername)
        val post = resolvePost(postId)
        val directChatKey = ChatRoom.generateDirectChatKey(requester.getId()!!, target.getId()!!)
        val room = chatRoomRepository.findByDirectChatKeyAndPostId(
            directChatKey = directChatKey,
            postId = post.getId()!!
        )
            ?: throw ApplicationException.of(
                CommonStatusCode.ENDPOINT_NOT_FOUND,
                "채팅방 생성 중 충돌이 발생했습니다."
            )

        ensureParticipantRows(room)

        val participant = reactivateParticipant(room, requester)

        return CreateChatRoomResponse.of(
            roomId = room.getId(),
            postId = room.post.getId(),
            participantUsername = target.username,
            existingRoom = true,
            clearBefore = participant.deletedAt
        )
    }

    private fun getVisibleParticipant(
        roomId: Long,
        username: String
    ): ChatRoomParticipant {

        val participant = findParticipant(
            roomId = roomId,
            username = username
        )
            ?: throw ApplicationException.of(
                CommonStatusCode.INVALID_ARGUMENT,
                "해당 채팅방에 접근할 수 없습니다."
            )

        if (!participant.visible) {

            throw ApplicationException.of(
                CommonStatusCode.INVALID_ARGUMENT,
                "나간 채팅방입니다."
            )
        }

        return participant
    }

    /**
     * 참여자 row를 찾는다. 없으면 과거 방일 수 있으므로 한 번 보정한 뒤 다시 찾는다.
     *
     * 보정은 참여자 테이블이 도입되기 전에 만들어진 방을 위한 호환 장치일 뿐이다.
     * 일회성 데이터 백필이 끝나면 `backfillParticipantRows*` 계열과 함께 지워도 된다.
     */
    private fun findParticipant(
        roomId: Long,
        username: String
    ): ChatRoomParticipant? {

        val participant = chatRoomParticipantRepository.findByRoomIdAndMemberUsername(roomId, username)

        if (participant != null) {

            return participant
        }

        backfillParticipantRows(roomId)

        return chatRoomParticipantRepository.findByRoomIdAndMemberUsername(roomId, username)
    }

    private fun findParticipant(
        roomId: Long,
        memberId: Long
    ): ChatRoomParticipant? {

        val participant = chatRoomParticipantRepository.findByRoomIdAndMemberId(roomId, memberId)

        if (participant != null) {

            return participant
        }

        backfillParticipantRows(roomId)

        return chatRoomParticipantRepository.findByRoomIdAndMemberId(roomId, memberId)
    }

    /**
     * 요청자가 아닌 상대 참여자. 1:1 방이므로 참여자는 항상 둘이다.
     */
    private fun getOtherMember(
        roomId: Long,
        username: String
    ): Member =
        chatRoomParticipantRepository.findAllByRoomId(roomId)
            .firstOrNull { it.member.username != username }
            ?.member
            ?: throw ApplicationException.of(
                CommonStatusCode.INVALID_ARGUMENT,
                "채팅방 참여자가 아닙니다."
            )

    private fun getMemberByUsername(
        username: String
    ): Member =
        memberRepository.findByUsername(username)
            ?: throw ApplicationException.of(
                CommonStatusCode.INVALID_ARGUMENT,
                "존재하지 않는 사용자입니다: $username"
            )

    /**
     * 게시글 없이는 채팅방을 만들 수 없다.
     * postId는 검증에서 이미 걸러지지만, 서비스로 직접 들어오는 경로를 위해 여기서도 막는다.
     */
    private fun resolvePost(
        postId: Long?
    ): Post {

        if (postId == null) {

            throw ApplicationException(PostStatusCode.INVALID_POST)
        }

        val post = postRepository.findPostById(postId)
            ?: throw ApplicationException(PostStatusCode.INVALID_POST)

        if (post.isDeleted) {

            throw ApplicationException(PostStatusCode.INVALID_POST)
        }

        return post
    }

    private fun isProfessional(
        member: Member
    ): Boolean =
        member.role.name == "PROFESSIONAL"

    private fun normalizeMessage(
        message: String?
    ): String {

        val normalizedMessage = message?.trim().orEmpty()

        if (normalizedMessage.isEmpty()) {

            return ""
        }

        if (normalizedMessage.length > 1000) {

            throw ApplicationException.of(
                CommonStatusCode.INVALID_ARGUMENT,
                "메시지는 1000자를 초과할 수 없습니다."
            )
        }

        return normalizedMessage
    }

    private fun normalizeAttachmentUrls(
        attachmentUrls: List<String>
    ): List<String> {

        if (attachmentUrls.size > 10) {

            throw ApplicationException.of(
                CommonStatusCode.INVALID_ARGUMENT,
                "파일은 최대 10개까지 첨부할 수 있습니다."
            )
        }

        return attachmentUrls.map { it.trim() }
            .filter { it.isNotBlank() }
            .onEach {

                if (!it.startsWith("/files/")) {

                    throw ApplicationException.of(
                        CommonStatusCode.INVALID_ARGUMENT,
                        "첨부 파일 형식이 올바르지 않습니다."
                    )
                }
            }
    }

    /**
     * 캐시는 TTL(3일) 안의 메시지를 전부 들고 있어 활발한 방이면 수천 건이 된다.
     * 읽기는 항상 최신 쪽에서 시작하므로 뒤에서 `chatMessageCacheScanSize`개만 가져온다.
     * 그보다 오래된 구간을 찾는 요청은 기존대로 DB로 내려간다.
     */
    private fun getCachedRoomMessages(
        roomId: Long
    ): List<ChatMessageResponse> {

        val cachedMessages = redisTemplate.opsForList().range(
            cacheKey(roomId),
            -chatMessageCacheScanSize,
            -1
        )
            ?: emptyList()

        return cachedMessages.mapNotNull {

            runCatching {
                objectMapper.readValue(it, ChatMessageResponse::class.java)
            }.getOrNull()
        }
    }

    private fun appendRoomMessage(
        roomId: Long,
        message: ChatMessageResponse
    ) {

        val key = cacheKey(roomId)

        redisTemplate.opsForList().rightPush(
            key,
            objectMapper.writeValueAsString(message)
        )

        redisTemplate.expire(
            key,
            chatMessageCacheTtlMillis,
            TimeUnit.MILLISECONDS
        )
    }

    /**
     * 메시지 식별자를 Redis 카운터에서 DB PK로 옮기면서 캐시 키 버전을 올렸다.
     * 구버전 키에 남은 방별 카운터 기반 id와 전역 PK가 섞이지 않게 하려는 것이고, 구버전 키는 TTL로 사라진다.
     */
    private fun cacheKey(
        roomId: Long
    ): String =
        "chat:room:$roomId:messages:v2"

    /**
     * DB 행을 응답으로 되돌린다. 결제 상세는 저장된 JSON에서 복원하되,
     * 형식이 깨진 과거 행 하나 때문에 대화 전체 조회가 실패하지 않도록 실패 시 null로 둔다.
     */
    private fun toResponse(
        chatMessage: ChatMessage
    ): ChatMessageResponse {

        val payment = chatMessage.paymentPayload?.let {

            runCatching {
                objectMapper.readValue(it, ChatPaymentResponse::class.java)
            }.getOrNull()
        }

        return ChatMessageResponse.of(
            chatMessage = chatMessage,
            payment = payment
        )
    }

    private fun normalizePageSize(
        size: Int
    ): Int =
        size.coerceIn(1, MAX_PAGE_SIZE)

    /**
     * 참여자 row가 없는 과거 방 하나를 보정한다.
     * 보정의 근거로만 `client` / `professional`을 읽고, 멤버십 판단에는 쓰지 않는다.
     */
    private fun backfillParticipantRows(
        roomId: Long
    ) {

        val room = chatRoomRepository.findByIdWithParticipants(roomId)
            ?: return

        ensureParticipantRows(room)
    }

    private fun backfillParticipantRowsForMember(
        username: String
    ) {

        ensureParticipantRowsInBatch(chatRoomRepository.findAllByParticipantUsername(username))
    }

    private fun ensureParticipantRows(
        room: ChatRoom
    ) {

        ensureParticipantRow(room, room.client)
        ensureParticipantRow(room, room.professional)
    }

    /**
     * 여러 방의 참여자 row를 한 번에 보정한다.
     *
     * 방마다 존재 여부를 조회하면 방 수에 비례해 쿼리가 늘어나므로(N+1),
     * 조회 1회로 기존 (roomId, memberId) 조합을 모두 읽고 없는 것만 한 번에 저장한다.
     */
    private fun ensureParticipantRowsInBatch(
        rooms: List<ChatRoom>
    ) {

        val roomIds = rooms.mapNotNull { it.getId() }

        if (roomIds.isEmpty()) {

            return
        }

        val existingPairs = chatRoomParticipantRepository.findRoomMemberIdsByRoomIds(roomIds)
            .map { it.getRoomId() to it.getMemberId() }
            .toSet()

        val missingParticipants = rooms.flatMap { room ->

            listOf(room.client, room.professional).mapNotNull { member ->

                val roomId = room.getId()
                val memberId = member.getId()

                if (roomId == null || memberId == null || (roomId to memberId) in existingPairs) {

                    null
                } else {

                    ChatRoomParticipant(
                        room = room,
                        member = member
                    )
                }
            }
        }

        if (missingParticipants.isEmpty()) {

            return
        }

        chatRoomParticipantRepository.saveAll(missingParticipants)
    }

    private fun ensureParticipantRow(
        room: ChatRoom,
        member: Member
    ) {

        if (chatRoomParticipantRepository.findByRoomIdAndMemberId(room.getId(), member.getId()) != null) {

            return
        }

        chatRoomParticipantRepository.save(
            ChatRoomParticipant(
                room = room,
                member = member
            )
        )
    }

    private fun reactivateParticipant(
        room: ChatRoom,
        member: Member
    ): ChatRoomParticipant {

        val participant = chatRoomParticipantRepository.findByRoomIdAndMemberId(
            roomId = room.getId(),
            memberId = member.getId()
        ) ?: throw ApplicationException.of(
            CommonStatusCode.ENDPOINT_NOT_FOUND,
            "채팅방 참여자 정보가 없습니다."
        )

        participant.reactivate()

        return participant
    }

    private fun reactivateParticipantsOnNewMessage(
        room: ChatRoom,
        senderId: Long?
    ) {

        val participants = chatRoomParticipantRepository.findAllByRoomId(room.getId()!!)

        for (participant in participants) {

            if (participant.member.getId() == senderId) {

                continue
            }

            participant.reactivate()
        }
    }

    companion object {

        private const val MAX_PAGE_SIZE = 100
    }
}