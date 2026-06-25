package spring.springserver.domain.chat.data.request

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class SendChatMessageRequest(
    @field:NotNull
    @field:Positive
    val roomId: Long,

    val message: String? = null,

    @field:Size(max = 4, message = "파일은 최대 4개까지 첨부할 수 있습니다.")
    val fileUrls: List<String> = emptyList()
)
