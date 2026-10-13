package spring.springserver.global.config

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "anthropic")
class AnthropicProperties {

    /**
     * Claude API 키. 비어 있어도 기동은 되지만 리뷰 AI 분석만 동작하지 않는다.
     * 기동을 막으면 이 기능과 무관한 배포까지 전부 실패하므로 경고만 남긴다.
     */
    var apiKey: String = ""

    var model: String = "claude-haiku-4-5"

    var maxTokens: Long = 2048

    /**
     * .env에서 넘어온 값에 공백이 섞여 들어오는 일이 잦아 쓰기 직전에 다듬는다.
     */
    fun getTrimmedApiKey(): String = apiKey.trim()

    fun isConfigured(): Boolean = getTrimmedApiKey().isNotBlank()

    /**
     * 키를 빼먹은 배포를 첫 호출(502)이 아니라 기동 로그에서 잡게 한다.
     */
    @PostConstruct
    fun warnIfNotConfigured() {

        if (!isConfigured()) {

            log.warn("ANTHROPIC_API_KEY가 설정되지 않았습니다. 리뷰 AI 분석 기능은 동작하지 않습니다.")
        }
    }

    companion object {

        private val log = LoggerFactory.getLogger(AnthropicProperties::class.java)
    }
}
