package spring.springserver.global.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "app.post.repeat-rate")
class PostRepeatRateProperties {

    /**
     * 재구매율 정렬에서 표본 보정 기준. 서로 다른 구매자 수가 이 값보다 작으면 정렬 시 0 으로 취급한다.
     * 응답에는 실제 값을 그대로 내려준다.
     */
    var minBuyers: Int = 3

    /**
     * 애플리케이션 시작 시 재구매율을 일괄 재계산한다. 신규 컬럼 도입 후 1회 켜고 끈다.
     */
    var backfillOnStart: Boolean = false
}
