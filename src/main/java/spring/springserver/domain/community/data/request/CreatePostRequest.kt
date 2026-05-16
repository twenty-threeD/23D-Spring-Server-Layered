package spring.springserver.domain.community.data.request

import jakarta.validation.constraints.NotBlank
import spring.springserver.domain.community.entity.CommunityPost
import spring.springserver.domain.member.entity.Member

data class CreatePostRequest(
    @field:NotBlank
    val title: String,
    val content: String?,
    val fileUrl: String?,
) {
    fun toEntity(member: Member): CommunityPost {
        return CommunityPost(
            member = member,
            username = member.username,
            title = title.trim(),
            content = content?.trim()?.takeIf { it.isNotBlank() },
            fileUrl = fileUrl?.trim()?.takeIf { it.isNotBlank() },
            viewCount = 0,
            isEdited = false,
        )
    }
}
