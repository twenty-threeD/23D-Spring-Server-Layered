package spring.springserver.global.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "toss-payments")
class TossPaymentsProperties {

    lateinit var secretKey: String
    var baseUrl: String = "https://api.tosspayments.com"
    var apiVersion: String = "2024-06-01"
}
