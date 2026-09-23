package spring.springserver.domain.profile.data.response

import spring.springserver.domain.post.data.response.PostResponse
import spring.springserver.domain.profile.entity.Profile
import java.time.LocalDateTime

data class ProfileResponse(
    val memberId: Long?,

    val username: String,

    val email: String,

    val phone: String?,

    val imageUrl: String?,

    val sigCd: String?,

    val locationName: String?,

    val movableDistance: String?,

    val movableDistanceLabel: String?,

    val shortDescription: String?,

    val jobCategoryId: Long?,

    val jobCategoryName: String?,

    val phoneVerified: Boolean,

    val posts: List<PostResponse>,

    /**
     * 전문가로서 받은 리뷰 수. 평점은 계약 기준으로 쌓이므로 게시글과 무관하다.
     */
    val reviewCount: Long,

    /**
     * 전문가로서 받은 평균 별점(소수 첫째 자리). 리뷰가 없으면 0.0이다.
     */
    val averageRating: Double,

    val updatedAt: LocalDateTime?
) {

    companion object {

        fun of(
            profile: Profile,
            memberId: Long?,
            username: String,
            email: String,
            phone: String?,
            locationName: String?,
            jobCategoryName: String?,
            phoneVerified: Boolean,
            posts: List<PostResponse>,
            reviewCount: Long,
            averageRating: Double
        ): ProfileResponse {

            return ProfileResponse(
                memberId,
                username,
                email,
                phone,
                profile.imageUrl,
                profile.sig?.getSigCd(),
                locationName,
                profile.movableDistance?.name,
                profile.movableDistance?.getLabel(),
                profile.shortDescription,
                profile.jobCategory?.getId(),
                jobCategoryName,
                phoneVerified,
                posts,
                reviewCount,
                averageRating,
                profile.getUpdatedAt()
            )
        }
    }
}
