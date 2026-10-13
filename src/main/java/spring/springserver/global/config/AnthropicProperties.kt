package spring.springserver.global.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "anthropic")
class AnthropicProperties {

    lateinit var apiKey: String
    var model: String = "claude-haiku-4-5"
    var maxTokens: Long = 2048
}
