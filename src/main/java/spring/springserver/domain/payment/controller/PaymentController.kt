package spring.springserver.domain.payment.controller

import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import spring.springserver.domain.blockchain.data.response.PaymentVerificationResponse
import spring.springserver.domain.payment.data.request.CancelPaymentRequest
import spring.springserver.domain.payment.data.request.ConfirmPaymentRequest
import spring.springserver.domain.payment.data.request.PreparePaymentRequest
import spring.springserver.domain.payment.data.request.VirtualAccountRequest
import spring.springserver.domain.payment.data.response.ConfirmPaymentResponse
import spring.springserver.domain.payment.data.response.PaymentResponse
import spring.springserver.domain.payment.data.response.PreparePaymentResponse
import spring.springserver.domain.payment.service.PaymentService
import spring.springserver.global.data.BaseResponse
import spring.springserver.global.jwt.MemberDetails

@RestController
@RequestMapping("/api/payment")
class PaymentController(
    private val paymentService: PaymentService
) {

    @PostMapping("/prepare")
    fun prepare(
        @Valid @RequestBody preparePaymentRequest: PreparePaymentRequest,
        @AuthenticationPrincipal memberDetails: MemberDetails
    ): BaseResponse<PreparePaymentResponse> {

        return BaseResponse.ok(
            paymentService.prepare(
                preparePaymentRequest,
                memberDetails.getId()!!
            )
        )
    }

    @PostMapping("/confirm")
    fun confirm(
        @Valid @RequestBody confirmPaymentRequest: ConfirmPaymentRequest,
        @AuthenticationPrincipal memberDetails: MemberDetails
    ): BaseResponse<ConfirmPaymentResponse> {

        return BaseResponse.ok(
            paymentService.confirm(
                confirmPaymentRequest,
                memberDetails.getId()!!
            )
        )
    }

    @GetMapping("/{paymentKey}")
    fun findByPaymentKey(
        @PathVariable paymentKey: String,
        @AuthenticationPrincipal memberDetails: MemberDetails
    ): BaseResponse<PaymentResponse> {

        return BaseResponse.ok(
            paymentService.findByPaymentKey(
                paymentKey,
                memberDetails.getId()!!
            )
        )
    }

    @GetMapping("/orders/{orderId}")
    fun findByOrderId(
        @PathVariable orderId: String,
        @AuthenticationPrincipal memberDetails: MemberDetails
    ): BaseResponse<PaymentResponse> {

        return BaseResponse.ok(
            paymentService.findByOrderId(
                orderId,
                memberDetails.getId()!!
            )
        )
    }

    @PostMapping("/{paymentKey}/cancel")
    fun cancel(
        @Valid @RequestBody cancelPaymentRequest: CancelPaymentRequest,
        @PathVariable paymentKey: String,
        @RequestHeader("Idempotency-Key", required = false) idempotencyKey: String?,
        @AuthenticationPrincipal memberDetails: MemberDetails
    ): BaseResponse<PaymentResponse> {

        return BaseResponse.ok(
            paymentService.cancel(
                cancelPaymentRequest,
                paymentKey,
                idempotencyKey,
                memberDetails.getId()!!
            )
        )
    }

    @PostMapping("/virtual-accounts")
    fun issueVirtualAccount(
        @Valid @RequestBody virtualAccountRequest: VirtualAccountRequest,
        @AuthenticationPrincipal memberDetails: MemberDetails
    ): BaseResponse<PaymentResponse> {

        return BaseResponse.ok(
            paymentService.issueVirtualAccount(
                virtualAccountRequest,
                memberDetails.getId()!!
            )
        )
    }

    @GetMapping("/orders/{orderId}/verify")
    fun verify(
        @PathVariable orderId: String,
        @AuthenticationPrincipal memberDetails: MemberDetails
    ): BaseResponse<PaymentVerificationResponse> {

        return BaseResponse.ok(
            paymentService.verify(
                orderId = orderId,
                memberId = memberDetails.getId()!!
            )
        )
    }
}