package spring.springserver.domain.push.client

import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import spring.springserver.global.config.ExpoPushProperties
import java.time.Duration

@Component
class ExpoPushClient(
    restClientBuilder: RestClient.Builder,
    private val expoPushProperties: ExpoPushProperties
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val restClient: RestClient = restClientBuilder
        .requestFactory(
            SimpleClientHttpRequestFactory().apply {

                setConnectTimeout(Duration.ofSeconds(3))
                setReadTimeout(Duration.ofSeconds(10))
            }
        )
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .build()

    /**
     * 메시지를 보내고, Expo가 DeviceNotRegistered로 거절한 토큰을 돌려준다.
     * Expo는 요청 하나에 최대 100건을 받으므로 나눠 보낸다. 묶음 하나가 실패해도 나머지는 보낸다.
     */
    fun send(
        expoPushMessages: List<ExpoPushMessage>
    ): List<String> {

        return expoPushMessages
            .chunked(MAX_MESSAGES_PER_REQUEST)
            .flatMap { chunk ->

                runCatching { sendChunk(chunk) }
                    .onFailure { throwable -> log.warn("Expo 푸시 발송 실패. count = {}", chunk.size, throwable) }
                    .getOrDefault(emptyList())
            }
    }

    private fun sendChunk(
        chunk: List<ExpoPushMessage>
    ): List<String> {

        val response = restClient
            .post()
            .uri(expoPushProperties.url)
            .headers { headers ->

                val accessToken = expoPushProperties.accessToken.trim()

                if (accessToken.isNotEmpty()) {

                    headers.setBearerAuth(accessToken)
                }
            }
            .body(chunk)
            .retrieve()
            .body<JsonNode>()

        val tickets = response?.get("data")
            ?: return emptyList()

        /**
         * 티켓은 보낸 메시지 순서와 같다. 에러 티켓만 골라 원인을 남기고, 기기 미등록 토큰은 지울 대상으로 모은다.
         */
        return chunk.indices.mapNotNull { index ->

            val ticket = tickets.get(index)

            if (ticket == null || ticket.path("status").asText() != "error") {

                return@mapNotNull null
            }

            val errorCode = ticket.path("details").path("error").asText()

            log.warn("Expo 푸시 티켓 에러. error = {}, message = {}", errorCode, ticket.path("message").asText())

            if (errorCode == DEVICE_NOT_REGISTERED) chunk[index].to else null
        }
    }

    companion object {

        private const val MAX_MESSAGES_PER_REQUEST = 100
        private const val DEVICE_NOT_REGISTERED = "DeviceNotRegistered"
    }
}
