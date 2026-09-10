package spring.springserver.domain.call.data.request

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import spring.springserver.domain.call.entity.CallType

data class StartCallRequest(
    /**
     * 통화를 시작할 채팅방. 통화 상대는 이 채팅방의 반대편 참여자로 정해진다.
     *
     * 타입을 nullable로 두는 것은 클라이언트가 null을 보냈을 때 역직렬화 단계에서
     * 터지지 않고 검증 단계에서 400으로 걸러지도록 하기 위한 것이다.
     */
    @field:NotNull(message = "채팅방 정보가 필요합니다.")
    @field:Positive
    val roomId: Long?,

    @field:NotNull(message = "통화 종류가 필요합니다.")
    val callType: CallType?
)
