package spring.springserver.domain.community.data.response

data class CommunityLikeResponse(
    val targetId: Long,

    val likeCount: Long,

    val message: String,
) {
    companion object {

        fun of(targetId: Long, likeCount: Long, message: String): CommunityLikeResponse {

            return CommunityLikeResponse(
                targetId,
                likeCount,
                message,
            )
        }
    }
}
