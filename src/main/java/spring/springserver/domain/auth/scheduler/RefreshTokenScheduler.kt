package spring.springserver.domain.auth.scheduler

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.auth.repository.RefreshTokenRepository
import java.time.LocalDateTime

@Component
class RefreshTokenScheduler(
    private val refreshTokenRepository: RefreshTokenRepository
) {

    private val log = LoggerFactory.getLogger(RefreshTokenScheduler::class.java)

    /**
     * 매 시 정각에 만료일(expiresAt)이 지난 리프레시 토큰을 정리한다.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional(rollbackFor = [Exception::class])
    fun deleteExpiredRefreshTokens() {

        val deleted = refreshTokenRepository.deleteAllExpired(LocalDateTime.now())

        if (deleted > 0) log.info("만료된 리프레시 토큰 {}건 삭제", deleted)
    }
}
