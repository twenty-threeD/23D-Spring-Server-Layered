package spring.springserver.domain.community.job.data.response

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 구인/구직 좋아요 응답.
 * 등록·취소를 멱등으로 처리하므로 결과 상태(isLiked)를 함께 내려 프론트가
 * 로컬 상태를 추측하지 않아도 되게 한다.
 *
 * 일반 커뮤니티의 CommunityLikeResponse와 따로 두는 이유는, 그쪽은 중복 요청을
 * 에러로 처리하는 계약이라 isLiked를 붙이면 의미가 어긋나기 때문이다.
 */
data class JobPostLikeResponse(
    val postId: Long,

    val likeCount: Long,

    @get:JsonProperty("isLiked")
    val isLiked: Boolean,

    val message: String,
) {

    companion object {

        fun of(
            postId: Long,
            likeCount: Long,
            isLiked: Boolean,
            message: String
        ): JobPostLikeResponse {

            return JobPostLikeResponse(
                postId,
                likeCount,
                isLiked,
                message,
            )
        }
    }
}
