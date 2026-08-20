package spring.springserver.domain.post.data.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.post.category.entity.PostCategory
import spring.springserver.domain.post.entity.Post
import java.time.LocalDateTime

data class CreatePostRequest(
    @field:NotBlank
    @field:Size(max = 255, message = "제목은 255자 이하여야 합니다.")
    val title: String,

    @field:NotBlank
    @field:Size(max = 2000, message = "내용은 2000자 이하여야 합니다.")
    val content: String,

    /**
     * 이미지는 1개 이상 4개 이하로 첨부해야 한다.
     * 타입을 nullable로 두는 것은 클라이언트가 null을 보냈을 때 역직렬화 단계에서
     * 터지지 않고 검증 단계에서 걸러지도록 하기 위한 것이다.
     */
    @field:NotEmpty(message = "이미지는 최소 1개 이상 첨부해야 합니다.")
    @field:Size(max = 4, message = "이미지는 최대 4개까지 첨부할 수 있습니다.")
    val fileUrls: List<String>? = null,

    @field:Positive
    val categoryId: Long?,

) {

    fun toEntity(
        member: Member,
        category: PostCategory?
    ): Post {

        return Post(
            title = title,
            content = content,
            updatedAt = LocalDateTime.now(),
            member = member,
            category = category,
        )
    }
}
