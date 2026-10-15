package spring.springserver.domain.chat.initializer

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ScanOptions
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.chat.data.response.ChatMessageResponse
import spring.springserver.domain.chat.entity.ChatMessage
import spring.springserver.domain.chat.repository.ChatMessageRepository
import spring.springserver.domain.chat.repository.ChatRoomRepository
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.member.repository.MemberRepository

/**
 * 메시지가 Redis에만 저장되던 시절의 캐시(v1 키)를 `chat_message` 테이블로 한 번 옮긴다.
 *
 * 메시지 식별자가 방별 Redis 카운터에서 전역 PK로 바뀌었기 때문에 v1 캐시의 id는 그대로 쓸 수 없고,
 * 여기서 새 PK를 받아 다시 적재한다. 커서 페이지네이션이 `id DESC`를 쓰므로 이관 행의 id가
 * 이후 들어올 새 메시지보다 작아야 하고, 그래서 트래픽이 붙기 전인 기동 시점에 실행한다.
 *
 * 이관이 끝난 방은 Redis 마커로 표시해 재기동 때 중복 적재하지 않는다.
 * v1 캐시는 3일 TTL로 스스로 사라지므로 이 러너는 그 이후 지워도 된다.
 */
@Component
class ChatMessageCacheBackfillRunner(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val memberRepository: MemberRepository,
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper
) : ApplicationRunner {

    companion object {

        private val log = LoggerFactory.getLogger(ChatMessageCacheBackfillRunner::class.java)

        private const val LEGACY_KEY_PATTERN = "chat:room:*:messages"
        private const val SCAN_COUNT = 100L
    }

    @Transactional(rollbackFor = [Exception::class])
    override fun run(
        args: ApplicationArguments
    ) {

        val legacyKeys = scanLegacyKeys()

        if (legacyKeys.isEmpty()) {

            return
        }

        val membersByUsername = mutableMapOf<String, Member?>()
        var migratedCount = 0

        for (legacyKey in legacyKeys) {

            val roomId = parseRoomId(legacyKey)
                ?: continue

            if (redisTemplate.opsForValue().setIfAbsent(migrationMarkerKey(roomId), "1") != true) {

                continue
            }

            migratedCount += migrateRoom(
                legacyKey = legacyKey,
                roomId = roomId,
                membersByUsername = membersByUsername
            )
        }

        if (migratedCount > 0) {

            log.info("Backfilled {} legacy chat message(s) from Redis into PostgreSQL.", migratedCount)
        }
    }

    private fun migrateRoom(
        legacyKey: String,
        roomId: Long,
        membersByUsername: MutableMap<String, Member?>
    ): Int {

        val room = chatRoomRepository.findById(roomId).orElse(null)
            ?: return 0

        val legacyMessages = (redisTemplate.opsForList().range(legacyKey, 0, -1) ?: emptyList())
            .mapNotNull {

                runCatching {
                    objectMapper.readValue(it, ChatMessageResponse::class.java)
                }.getOrNull()
            }
            .sortedBy { it.createdAt }

        if (legacyMessages.isEmpty()) {

            return 0
        }

        val migrated = legacyMessages.mapNotNull { legacyMessage ->

            val sender = membersByUsername.getOrPut(legacyMessage.senderUsername) {

                memberRepository.findByUsername(legacyMessage.senderUsername)
            }
                ?: return@mapNotNull null

            ChatMessage(
                room = room,
                sender = sender,
                message = legacyMessage.message,
                createdAt = legacyMessage.createdAt,
                messageType = legacyMessage.type,
                attachmentUrlsText = ChatMessage.joinAttachmentUrls(legacyMessage.attachedFileUrls),
                paymentPayload = legacyMessage.payment?.let { objectMapper.writeValueAsString(it) }
            )
        }

        chatMessageRepository.saveAll(migrated)

        return migrated.size
    }

    /**
     * 운영 중인 Redis를 멈추지 않으려고 KEYS 대신 SCAN으로 훑는다.
     * v2 키는 `:messages:v2`로 끝나므로 이 패턴에 걸리지 않는다.
     */
    private fun scanLegacyKeys(): List<String> {

        val scanOptions = ScanOptions.scanOptions()
            .match(LEGACY_KEY_PATTERN)
            .count(SCAN_COUNT)
            .build()

        return redisTemplate.scan(scanOptions).use { cursor ->

            cursor.asSequence().toList()
        }
    }

    private fun parseRoomId(
        legacyKey: String
    ): Long? =
        legacyKey.removePrefix("chat:room:")
            .removeSuffix(":messages")
            .toLongOrNull()

    private fun migrationMarkerKey(
        roomId: Long
    ): String =
        "chat:room:$roomId:messages:migrated-to-db"
}
