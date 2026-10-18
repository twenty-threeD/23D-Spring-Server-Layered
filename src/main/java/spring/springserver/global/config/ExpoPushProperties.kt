package spring.springserver.global.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "expo.push")
class ExpoPushProperties {

    /**
     * Expo Push API 주소.
     */
    var url: String = "https://exp.host/--/api/v2/push/send"

    /**
     * Expo 대시보드에서 "Enhanced Security for Push Notifications"를 켰을 때만 필요한 액세스 토큰.
     * 비어 있으면 Authorization 헤더를 보내지 않는다.
     */
    var accessToken: String = ""

    /**
     * false면 발송을 건너뛴다. 로컬·테스트 환경에서 실제 기기로 푸시가 나가지 않게 막는다.
     */
    var enabled: Boolean = true
}
