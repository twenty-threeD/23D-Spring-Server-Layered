package spring.springserver.domain.post.data.response

import spring.springserver.domain.member.entity.Member

data class PostMemberResponse(
    val id: Long?,

    val username: String,

    val name: String,

    val imageUrl: String?,
) {

    companion object {

        fun of(
            member: Member,
            imageUrl: String?
        ): PostMemberResponse {

            // username/name은 Kotlin에서 non-null이지만 DB 컬럼에는 NOT NULL이 없어
            // 예전에 저장된 회원은 값이 비어 있을 수 있다. 그대로 넘기면 생성자
            // null 검사에 걸려 500이 되므로 빈 문자열로 낮춰 응답한다.
            return PostMemberResponse(
                member.getId(),
                member.username.orEmpty(),
                member.name.orEmpty(),
                imageUrl
            )
        }
    }
}