package spring.springserver.global.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "agora")
class AgoraProperties {

    /**
     * Agora 콘솔의 App ID. 클라이언트에게 그대로 내려준다.
     */
    var appId: String = ""

    /**
     * Agora 콘솔의 App Certificate. 토큰 서명에만 쓰이며 절대 응답에 담지 않는다.
     */
    var appCertificate: String = ""

    /**
     * 발급한 RTC 토큰의 유효 시간(초). 통화가 길어지면 클라이언트가 갱신 API로 재발급받는다.
     */
    var tokenExpirationSeconds: Int = 3600

    /**
     * 수신자가 응답하지 않은 통화를 부재중으로 정리하기까지의 시간(초).
     */
    var ringingTimeoutSeconds: Long = 60

    /**
     * 연결된 통화를 강제로 종료시키기까지의 시간(초).
     *
     * 클라이언트가 종료 API를 부르지 못하고 죽으면 통화가 ACCEPTED로 남아
     * "이미 진행 중인 통화" 때문에 두 사람 다 새 통화를 못 걸게 된다. 그 상태를 풀어주는 안전장치다.
     */
    var maxCallDurationSeconds: Long = 14400

    /**
     * .env에서 넘어온 값에 공백이 섞여 들어오는 일이 잦아 쓰기 직전에 다듬는다.
     */
    fun getTrimmedAppId(): String = appId.trim()

    fun getTrimmedAppCertificate(): String = appCertificate.trim()
}
