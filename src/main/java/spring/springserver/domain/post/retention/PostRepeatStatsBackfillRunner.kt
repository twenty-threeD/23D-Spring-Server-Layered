package spring.springserver.domain.post.retention

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import spring.springserver.domain.post.service.PostRepeatStatsService
import spring.springserver.global.config.PostRepeatRateProperties

/**
 * 재구매율 컬럼이 처음 도입되면 기존 글들은 0 으로 남는다.
 * 설정 플래그가 켜져 있을 때 애플리케이션 시작 직후 1회 전체 재계산을 돌린다.
 * 한 번 백필하면 설정을 다시 끄고 재배포한다.
 */
@Component
class PostRepeatStatsBackfillRunner(
    private val postRepeatStatsService: PostRepeatStatsService,
    private val postRepeatRateProperties: PostRepeatRateProperties
): ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(
        args: ApplicationArguments
    ) {

        if (!postRepeatRateProperties.backfillOnStart) {

            return
        }

        log.info("재구매율 백필을 시작한다. 설정에서 끄기 전까지 매 부팅마다 반복된다.")

        val size = postRepeatStatsService.recomputeAll()

        log.info("재구매율 백필 완료. 재계산한 post 수 = {}", size)
    }
}
