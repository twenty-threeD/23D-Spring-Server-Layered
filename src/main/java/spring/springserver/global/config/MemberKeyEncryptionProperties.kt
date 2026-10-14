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
}