package spring.springserver.global.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfig {

    /**
     * 매 요청마다 조회하는 사용자 정보처럼, 짧은 시간 안에 반복되는 조회만 로컬 메모리에서 돌려주려는 캐시다.
     * TTL이 길면 탈퇴/권한 변경이 늦게 반영되므로 60초로 짧게 둔다.
     */
    @Bean
    fun cacheManager(): CacheManager {

        val caffeineCacheManager = CaffeineCacheManager(MEMBER_DETAILS)

        caffeineCacheManager.setCaffeine(
            Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(60))
                .maximumSize(10_000)
        )

        return caffeineCacheManager
    }

    companion object {

        const val MEMBER_DETAILS = "memberDetails"
    }
}
