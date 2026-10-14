package spring.springserver.domain.community.post.data.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import spring.springserver.domain.community.post.entity.Category

data class UpdatePostRequest(
    val postId: Long,

    @field:NotBlank
    val title: String,

    val content: String?,

    val category: Category,

    @field:Pattern(
        regexp = "^\\s*(/files/[^/\\s]+)?\\s*$",
        message = "첨부 파일은 업로드한 파일 경로(/files/...)만 사용할 수 있습니다."
    )
    val fileUrl: String?,
)