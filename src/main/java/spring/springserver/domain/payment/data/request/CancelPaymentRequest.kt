package spring.springserver.domain.payment.data.request

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CancelPaymentRequest(
    @field:NotBlank(message = "cancelReason은 필수입니다.")
    @field:Size(max = 200, message = "cancelReason은 200자 이하여야 합니다.")
    val cancelReason: String,

    @field:Min(value = 1, message = "cancelAmount는 1 이상이어야 합니다.")
    val cancelAmount: Long? = null,

    @field:Valid
    val refundReceiveAccount: RefundReceiveAccountRequest? = null,

    @field:Min(value = 0, message = "taxFreeAmount는 0 이상이어야 합니다.")
    val taxFreeAmount: Long? = null,

    val currency: String? = null,

    @Deprecated("토스페이먼츠에서 deprecated된 필드입니다. 멱등키 사용을 권장합니다.")
    @field:Min(value = 0, message = "refundableAmount는 0 이상이어야 합니다.")
    val refundableAmount: Long? = null
)

data class RefundReceiveAccountRequest(
    @field:NotBlank(message = "bank는 필수입니다.")
    val bank: String,

    @field:NotBlank(message = "accountNumber는 필수입니다.")
    @field:Pattern(regexp = "^\\d+$", message = "accountNumber는 숫자만 입력해야 합니다.")
    val accountNumber: String,

    @field:NotBlank(message = "holderName은 필수입니다.")
    val holderName: String
)
