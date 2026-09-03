package spring.springserver.domain.post.data.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class UpdatePostRequest(
    @field:NotNull
    val id: Long,

    @field:NotBlank
    @field:Size(max = 255, message = "제목은 255자 이하여야 합니다.")
    val title: String,

    @field:NotBlank
    @field:Size(max = 2000, message = "내용은 2000자 이하여야 합니다.")
    val content: String,

    /**
     * null이면 기존 첨부를 그대로 두고, 값이 오면 그 목록으로 통째로 교체한다.
     * 게시글에는 이미지가 최소 1개 있어야 하므로 빈 배열은 거부한다.
     */
    @field:Size(max = 6, message = "이미지는 최대 6개까지 첨부할 수 있습니다.")
    val fileUrls: List<String>? = null,

    @field:Positive
    val categoryId: Long?,
)
