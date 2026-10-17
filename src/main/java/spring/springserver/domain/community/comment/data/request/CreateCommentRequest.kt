package spring.springserver.domain.community.comment.data.request

import jakarta.validation.constraints.NotBlank

data class CreateCommentRequest(
    val postId: Long,

    @field:NotBlank
    val content: String,

    /**
     * 값이 있으면 그 댓글의 답글이다. 서버는 깊이 1단계만 허용하므로 parent 가 이미 답글이면 거부한다.
     */
    val parentId: Long? = null,
)
