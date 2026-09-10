package spring.springserver.domain.call.data.request

import jakarta.validation.constraints.NotNull

/**
 * 통화 중 마이크/카메라 송출 상태 갱신 요청.
 *
 * 음성으로 시작한 통화에서 `videoEnabled = true`를 보내면 그대로 화상 통화가 된다.
 * 토큰을 다시 받을 필요는 없다.
 */
data class UpdateCallMediaRequest(
    @field:NotNull(message = "마이크 상태가 필요합니다.")
    val audioEnabled: Boolean?,

    @field:NotNull(message = "카메라 상태가 필요합니다.")
    val videoEnabled: Boolean?
)
