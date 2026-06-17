package spring.springserver.domain.payment.client

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import spring.springserver.domain.payment.data.request.CancelPaymentRequest
import spring.springserver.domain.payment.data.request.ConfirmPaymentRequest
import spring.springserver.domain.payment.data.request.VirtualAccountRequest
import spring.springserver.domain.payment.data.response.PaymentResponse
import spring.springserver.domain.payment.exception.PaymentStatusCode
import spring.springserver.global.config.TossPaymentsProperties
import spring.springserver.global.exception.exception.ApplicationException

@Component
class TossPaymentsClient(
    restClientBuilder: RestClient.Builder,
    private val tossPaymentsProperties: TossPaymentsProperties,
    private val objectMapper: ObjectMapper
) {

    private val restClient: RestClient = restClientBuilder
        .baseUrl(tossPaymentsProperties.baseUrl)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader("TossPayments-Version", tossPaymentsProperties.apiVersion)
        .build()

    fun confirm(confirmPaymentRequest: ConfirmPaymentRequest): PaymentResponse {

        return post("/v1/payments/confirm", confirmPaymentRequest)
    }

    fun findByPaymentKey(paymentKey: String): PaymentResponse {

        return get("/v1/payments/{paymentKey}", paymentKey)
    }

    fun findByOrderId(orderId: String): PaymentResponse {

        return get("/v1/payments/orders/{orderId}", orderId)
    }

    fun cancel(
        paymentKey: String,
        cancelPaymentRequest: CancelPaymentRequest,
        idempotencyKey: String?
    ): PaymentResponse {

        return execute {
            val requestBodySpec = restClient
                .post()
                .uri("/v1/payments/{paymentKey}/cancel", paymentKey)
                .headers { headers -> applyAuthorization(headers) }
                .headers { headers ->
                    if (!idempotencyKey.isNullOrBlank()) {
                        headers.set("Idempotency-Key", idempotencyKey)
                    }
                }
                .body(cancelPaymentRequest)

            requestBodySpec.retrieve().body(JsonNode::class.java)?.let(PaymentResponse::of)
                ?: PaymentResponse.of(objectMapper.createObjectNode())
        }
    }

    fun issueVirtualAccount(virtualAccountRequest: VirtualAccountRequest): PaymentResponse {

        return post("/v1/virtual-accounts", virtualAccountRequest)
    }

    private fun get(
        uri: String,
        uriVariable: String
    ): PaymentResponse {

        return execute {
            restClient
                .get()
                .uri(uri, uriVariable)
                .headers { headers -> applyAuthorization(headers) }
                .retrieve()
                .body(JsonNode::class.java)?.let(PaymentResponse::of)
                ?: PaymentResponse.of(objectMapper.createObjectNode())
        }
    }

    private fun post(
        uri: String,
        body: Any
    ): PaymentResponse {

        return execute {
            restClient
                .post()
                .uri(uri)
                .headers { headers -> applyAuthorization(headers) }
                .body(body)
                .retrieve()
                .body(JsonNode::class.java)?.let(PaymentResponse::of)
                ?: PaymentResponse.of(objectMapper.createObjectNode())
        }
    }

    private fun applyAuthorization(headers: HttpHeaders) {

        val secretKey = tossPaymentsProperties.secretKey
        if (secretKey.isBlank()) {
            throw ApplicationException(PaymentStatusCode.TOSS_PAYMENTS_SECRET_KEY_MISSING)
        }

        headers.setBasicAuth(secretKey, "")
    }

    private fun execute(request: () -> PaymentResponse?): PaymentResponse {

        try {
            return request()
                ?: PaymentResponse.of(objectMapper.createObjectNode())
        } catch (exception: RestClientResponseException) {
            val tossError = parseTossError(exception)
            val message = tossError?.let { "${it.code}: ${it.message}" }
                ?: if (exception.statusCode.is4xxClientError) {
                    PaymentStatusCode.TOSS_PAYMENTS_REQUEST_INVALID.message
                } else {
                    PaymentStatusCode.TOSS_PAYMENTS_REQUEST_FAILED.message
                }

            val statusCode = if (exception.statusCode.is4xxClientError) {
                PaymentStatusCode.TOSS_PAYMENTS_REQUEST_INVALID
            } else {
                PaymentStatusCode.TOSS_PAYMENTS_REQUEST_FAILED
            }

            throw ApplicationException(statusCode, message)
        } catch (exception: RestClientException) {
            throw ApplicationException(
                PaymentStatusCode.TOSS_PAYMENTS_REQUEST_FAILED,
                exception.message ?: PaymentStatusCode.TOSS_PAYMENTS_REQUEST_FAILED.message
            )
        }
    }

    private fun parseTossError(exception: RestClientResponseException): TossPaymentsError? {

        return runCatching {
            objectMapper.readValue(
                exception.responseBodyAsByteArray,
                TossPaymentsError::class.java
            )
        }.getOrNull()
    }
}

data class TossPaymentsError(
    val code: String? = null,
    val message: String? = null
)
