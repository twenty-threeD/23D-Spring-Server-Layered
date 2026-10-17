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

    /**
     * 답글에 다시 답할 때 그 대상 댓글 id. parentId 와 함께 보내야 하고, 원댓글이나 같은 원댓글의 답글이어야 한다.
     */
    val replyToCommentId: Long? = null,
)
