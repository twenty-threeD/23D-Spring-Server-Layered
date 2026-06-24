package spring.springserver.domain.chat.data.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class SendChatMessageRequest(
    @field:NotNull
    @field:Positive
    val roomId: Long,

    @field:NotBlank
    val message: String
)
