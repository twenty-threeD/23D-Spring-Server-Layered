package spring.springserver.domain.chat.data.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class CreateChatRoomRequest(
    @field:NotBlank
    val username: String,

    /**
     * 채팅을 시작한 게시글. 게시글 없이는 채팅방을 만들 수 없다.
     * 타입을 nullable로 두는 것은 클라이언트가 null을 보냈을 때 역직렬화 단계에서
     * 터지지 않고 검증 단계에서 400으로 걸러지도록 하기 위한 것이다.
     */
    @field:NotNull(message = "게시글 정보가 필요합니다.")
    @field:Positive
    val postId: Long?
)
