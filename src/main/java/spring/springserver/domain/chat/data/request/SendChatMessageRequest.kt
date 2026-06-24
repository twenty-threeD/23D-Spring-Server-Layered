package spring.springserver.domain.chat.data.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class SendChatMessageRequest(
    @field:NotNull
    var roomId: Long,

    @field:NotBlank
    val message: String
)
