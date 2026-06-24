package spring.springserver.domain.payment.controller

import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import spring.springserver.domain.payment.data.request.CancelPaymentRequest
import spring.springserver.domain.payment.data.request.ConfirmPaymentRequest
import spring.springserver.domain.payment.data.request.VirtualAccountRequest
import spring.springserver.domain.payment.data.response.PaymentResponse
import spring.springserver.domain.payment.service.PaymentService
import spring.springserver.global.data.BaseResponse
import spring.springserver.global.jwt.MemberDetails

@RestController
@RequestMapping("/api/payment")
class PaymentController(
    private val paymentService: PaymentService
) {

    @PostMapping("/confirm")
    fun confirm(
        @Valid @RequestBody confirmPaymentRequest: ConfirmPaymentRequest,
        @AuthenticationPrincipal memberDetails: MemberDetails
    ): BaseResponse<PaymentResponse> {

        return BaseResponse.ok(
            paymentService.confirm(
                confirmPaymentRequest,
                memberDetails.getId()!!
            )
        )
    }

    @GetMapping("/{paymentKey}")
    fun findByPaymentKey(
        @PathVariable paymentKey: String
    ): BaseResponse<PaymentResponse> {

        return BaseResponse.ok(paymentService.findByPaymentKey(paymentKey))
    }

    @GetMapping("/orders/{orderId}")
    fun findByOrderId(
        @PathVariable orderId: String
    ): BaseResponse<PaymentResponse> {

        return BaseResponse.ok(paymentService.findByOrderId(orderId))
    }

    @PostMapping("/{paymentKey}/cancel")
    fun cancel(
        @PathVariable paymentKey: String,
        @Valid @RequestBody cancelPaymentRequest: CancelPaymentRequest,
        @RequestHeader("Idempotency-Key", required = false) idempotencyKey: String?
    ): BaseResponse<PaymentResponse> {

        return BaseResponse.ok(
            paymentService.cancel(
                paymentKey,
                cancelPaymentRequest,
                idempotencyKey
            )
        )
    }

    @PostMapping("/virtual-accounts")
    fun issueVirtualAccount(
        @Valid @RequestBody virtualAccountRequest: VirtualAccountRequest
    ): BaseResponse<PaymentResponse> {

        return BaseResponse.ok(paymentService.issueVirtualAccount(virtualAccountRequest))
    }
}
