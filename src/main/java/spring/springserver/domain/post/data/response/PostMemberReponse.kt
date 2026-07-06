package spring.springserver.domain.post.data.response

import spring.springserver.domain.member.entity.Member

data class PostMemberResponse(
    val id: Long?,

    val username: String,

    val name: String,
) {

    companion object {

        fun of(
            member: Member
        ): PostMemberResponse {

            return PostMemberResponse(
                member.getId(),
                member.username,
                member.name
            )
        }
    }
}