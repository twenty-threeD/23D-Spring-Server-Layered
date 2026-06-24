package spring.springserver.global.config.blockchain

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "cosmos")
class CosmosProperties {

    lateinit var submitterPrivateKey: String
    lateinit var nodeUrl: String
    lateinit var chainId: String
}