package spring.springserver.domain.community.post.data.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import spring.springserver.domain.community.post.entity.Category
import spring.springserver.domain.community.post.entity.CommunityPost
import spring.springserver.domain.member.entity.Member

data class CreatePostRequest(
    @field:NotBlank
    val title: String,

    val content: String?,

    @field:Pattern(
        regexp = "^\\s*(/files/[^/\\s]+)?\\s*$",
        message = "첨부 파일은 업로드한 파일 경로(/files/...)만 사용할 수 있습니다."
    )
    val fileUrl: String?,

    val category: Category,
) {
    fun toEntity(
        member: Member
    ): CommunityPost {

        return CommunityPost(
            member = member,
            username = member.username,
            title = title.trim(),
            content = content?.trim()?.takeIf { it.isNotBlank() },
            fileUrl = fileUrl?.trim()?.takeIf { it.isNotBlank() },
            viewCount = 0,
            category = category,
            isEdited = false,
        )
    }
}