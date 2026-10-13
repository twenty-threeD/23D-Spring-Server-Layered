package spring.springserver.domain.chat.data.request

import jakarta.validation.constraints.NotNull
import spring.springserver.domain.chat.entity.TradeStatus

data class UpdateTradeStatusRequest(
    /**
     * 사용자가 직접 바꿀 수 있는 값은 CANCELED뿐이다.
     * 완료는 결제 승인으로, 진행 중은 새 계약서 등록으로만 바뀐다.
     */
    @field:NotNull(message = "거래 상태는 필수입니다.")
    val status: TradeStatus?
)