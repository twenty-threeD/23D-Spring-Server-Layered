package spring.springserver.domain.blockchain.controller

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import spring.springserver.domain.blockchain.data.response.TxVerificationResponse
import spring.springserver.domain.blockchain.service.TxVerificationService
import spring.springserver.global.data.BaseResponse
import spring.springserver.global.jwt.MemberDetails

@RestController
@RequestMapping("/api/blockchain")
class BlockchainController(
    private val txVerificationService: TxVerificationService
) {

    @GetMapping("/verify/{txHash}")
    fun verifyByTxHash(
        @PathVariable txHash: String,
        @AuthenticationPrincipal memberDetails: MemberDetails?
    ): BaseResponse<TxVerificationResponse> {

        return BaseResponse.ok(
            txVerificationService.verifyByTxHash(
                txHash = txHash,
                memberId = memberDetails?.getId()
            )
        )
    }
}


