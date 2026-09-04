package spring.springserver.global.util

import org.springframework.stereotype.Component
import spring.springserver.global.config.blockchain.CosmosProperties
import java.security.MessageDigest

@Component
class ContractUrlHasher(private val cosmosProperties: CosmosProperties) {

    fun hash(
        contractUrl: String
    ): String {

        return MessageDigest
            .getInstance("SHA-256")
            .digest("${cosmosProperties.contractUrlSalt} | $contractUrl".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    fun matches(
        contractUrl: String,
        contractUrlHash: String
    ): Boolean {

        return hash(contractUrl = contractUrl) == contractUrlHash
    }
}