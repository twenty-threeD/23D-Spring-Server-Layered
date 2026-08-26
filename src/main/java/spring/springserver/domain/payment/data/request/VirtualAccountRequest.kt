package spring.springserver.domain.payment.data.request

import jakarta.validation.*
import jakarta.validation.constraints.*
import kotlin.reflect.KClass

@ValidVirtualAccountTiming
data class VirtualAccountRequest(
    @field:Min(value = 1, message = "amount는 1 이상이어야 합니다.")
    val amount: Long,

    @field:NotBlank(message = "orderId는 필수입니다.")
    @field:Size(min = 6, max = 64, message = "orderId는 6자 이상 64자 이하여야 합니다.")
    val orderId: String,

    @field:NotBlank(message = "orderName은 필수입니다.")
    @field:Size(max = 100, message = "orderName은 100자 이하여야 합니다.")
    val orderName: String,

    @field:NotBlank(message = "customerName은 필수입니다.")
    @field:Size(max = 100, message = "customerName은 100자 이하여야 합니다.")
    val customerName: String,

    @field:NotBlank(message = "bank는 필수입니다.")
    val bank: String,

    @field:Min(value = 1, message = "validHours는 1 이상이어야 합니다.")
    @field:Max(value = 2160, message = "validHours는 2160 이하여야 합니다.")
    val validHours: Int? = null,

    @field:Pattern(
        regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$",
        message = "dueDate는 yyyy-MM-dd'T'HH:mm:ss 형식이어야 합니다."
    )
    val dueDate: String? = null,

    val customerEmail: String? = null,
    val customerMobilePhone: String? = null,

    @field:Min(value = 0, message = "taxFreeAmount는 0 이상이어야 합니다.")
    val taxFreeAmount: Long? = null,

    val useEscrow: Boolean? = null,

    @field:Valid
    val cashReceipt: VirtualAccountCashReceiptRequest? = null,

    @field:Valid
    val escrowProducts: List<EscrowProductRequest>? = null
)

data class VirtualAccountCashReceiptRequest(
    @field:NotBlank(message = "type은 필수입니다.")
    val type: String,

    val registrationNumber: String? = null
)

data class EscrowProductRequest(
    @field:NotBlank(message = "id는 필수입니다.")
    val id: String,

    @field:NotBlank(message = "name은 필수입니다.")
    val name: String,

    @field:NotBlank(message = "code는 필수입니다.")
    val code: String,

    @field:Min(value = 1, message = "unitPrice는 1 이상이어야 합니다.")
    val unitPrice: Long,

    @field:Min(value = 1, message = "quantity는 1 이상이어야 합니다.")
    val quantity: Int
)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ValidVirtualAccountTimingValidator::class])
annotation class ValidVirtualAccountTiming(
    val message: String = "validHours와 dueDate는 동시에 사용할 수 없습니다.",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class ValidVirtualAccountTimingValidator : ConstraintValidator<ValidVirtualAccountTiming, VirtualAccountRequest> {

    override fun isValid(
        value: VirtualAccountRequest?,
        context: ConstraintValidatorContext
    ): Boolean {

        if (value == null) {
            return true
        }

        val hasValidHours = value.validHours != null
        val hasDueDate = value.dueDate != null

        if (hasValidHours && hasDueDate) {
            context.disableDefaultConstraintViolation()
            context.buildConstraintViolationWithTemplate(
                "validHours와 dueDate는 동시에 사용할 수 없습니다."
            ).addPropertyNode("dueDate").addConstraintViolation()
            return false
        }

        return true
    }
}
