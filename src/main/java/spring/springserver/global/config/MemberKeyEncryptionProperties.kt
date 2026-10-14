package spring.springserver.global.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "member-key")
class MemberKeyEncryptionProperties {

    /**
     * 회원 서명용 개인키를 암호화하는 AES-256 마스터키(Base64, 32바이트).
     * DB·.env와 분리된 시크릿으로 주입하며, 잃어버리면 모든 회원 개인키를 복구할 수 없다.
     */
    var masterKey: String = ""

    /**
     * true면 기동 시 남아 있는 평문 개인키를 일괄 암호화하고, 이후 평문 개인키를 거부한다.
     * Blue-Green 전환 중 구버전 컨테이너가 평문/암호문을 모두 읽을 수 있도록, 이 코드가 한 번 배포된 뒤에 켠다.
     */
    var encryptPlaintext: Boolean = false
}