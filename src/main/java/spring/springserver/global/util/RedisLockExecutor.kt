package spring.springserver.global.util

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

/**
 * 인스턴스가 여러 대여도 한 대에서만 실행되어야 하는 작업을 감싸는 Redis 잠금.
 *
 * 스케줄러처럼 모든 인스턴스가 같은 시각에 같은 일을 하려 드는 자리에 쓴다.
 */
@Component
class RedisLockExecutor(
    private val redisTemplate: RedisTemplate<String, String>
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 잠금을 잡은 인스턴스만 지우도록 값을 비교한 뒤 삭제한다.
     * 비교와 삭제를 나눠 하면 잠금이 만료된 직후 남이 새로 잡은 잠금을 지울 수 있다.
     */
    private val releaseScript = DefaultRedisScript(
        """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            end
            return 0
            """,
        Long::class.java
    )

    /**
     * 잠금을 잡았으면 [action]을 실행하고 그 결과를, 잡지 못했으면 null을 돌려준다.
     *
     * @param ttl 실행 중 인스턴스가 죽어도 잠금이 영구히 남지 않도록 하는 만료 시간.
     *            작업의 최대 소요 시간보다 넉넉하게 잡는다.
     */
    fun <T> runWithLock(
        key: String,
        ttl: Duration,
        action: () -> T
    ): T? {

        val lockToken = UUID.randomUUID().toString()

        val acquired = redisTemplate.opsForValue().setIfAbsent(
            key,
            lockToken,
            ttl
        ) ?: false

        if (!acquired) {

            return null
        }

        try {

            return action()
        } finally {

            release(
                key = key,
                lockToken = lockToken
            )
        }
    }

    private fun release(
        key: String,
        lockToken: String
    ) {

        runCatching {

            redisTemplate.execute(
                releaseScript,
                listOf(key),
                lockToken
            )
        }.onFailure { throwable ->

            log.warn("Redis 잠금 해제 실패. key={} TTL로 자동 해제됩니다.", key, throwable)
        }
    }
}
