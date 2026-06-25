package spring.springserver.domain.chat.data.request

import jakarta.validation.constraints.NotBlank

data class CreateChatRoomRequest(
    @field:NotBlank
    val username: String
)
