package spring.springserver.domain.payment.data.request

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ConfirmPaymentRequest(
    @field:NotBlank(message = "paymentKey는 필수입니다.")
    val paymentKey: String,

    @field:NotBlank(message = "orderId는 필수입니다.")
    @field:Size(min = 6, max = 64, message = "orderId는 6자 이상 64자 이하여야 합니다.")
    val orderId: String,

    @field:Min(value = 1, message = "amount는 1 이상이어야 합니다.")
    val amount: Long,

    /**
     * 견적서 결제인 경우에만 전달한다.
     * 결제가 승인되면 해당 견적서는 결제 완료 처리되어 수정·삭제가 막힌다.
     */
    val estimateId: Long? = null
)
