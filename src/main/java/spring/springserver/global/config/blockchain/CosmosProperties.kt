package spring.springserver.global.config.blockchain

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "cosmos")
class CosmosProperties {

    var submitterPrivateKey: String = ""
    var submitterPrivateKeys: List<String> = emptyList()
    lateinit var nodeUrl: String
    lateinit var chainId: String
    lateinit var contractUrlSalt: String

    /**
     * 결제 기록에 서명할 개인키 목록. 단일 키 설정만 있는 기존 환경도 그대로 동작하도록 둘을 합친다.
     */
    fun allSubmitterPrivateKeys(): List<String> {

        return (submitterPrivateKeys + submitterPrivateKey)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }
}