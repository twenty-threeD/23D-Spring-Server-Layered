package spring.springserver.domain.post.data.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.post.entity.Post
import java.time.LocalDateTime

data class CreatePostRequest(
    @field:NotBlank
    @field:Size(max = 255, message = "제목은 255자 이하여야 합니다.")
    val title: String,

    @field:NotBlank
    @field:Size(max = 2000, message = "내용은 2000자 이하여야 합니다.")
    val content: String,

) {

    fun toEntity(member: Member): Post {

        return Post(
            title = title,
            content = content,
            updatedAt = LocalDateTime.now(),
            member = member,
        )
    }
}
