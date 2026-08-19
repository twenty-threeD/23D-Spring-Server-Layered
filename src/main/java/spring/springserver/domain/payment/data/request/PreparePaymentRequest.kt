package spring.springserver.domain.payment.data.request

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class PreparePaymentRequest(
    @field:NotBlank(message = "orderId는 필수입니다.")
    @field:Size(min = 6, max = 64, message = "orderId는 6자 이상 64자 이하여야 합니다.")
    val orderId: String,

    @field:Min(value = 1, message = "amount는 1 이상이어야 합니다.")
    val amount: Long,

    @field:NotBlank(message = "contractUrl은 필수입니다.")
    @field:Size(max = 2048, message = "contractUrl은 2048자 이하여야 합니다.")
    val contractUrl: String,

    @field:Size(max = 100, message = "orderName은 100자 이하여야 합니다.")
    val orderName: String? = null
)