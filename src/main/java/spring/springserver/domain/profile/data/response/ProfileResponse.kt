package spring.springserver.domain.profile.data.response

import spring.springserver.domain.post.data.response.PostResponse
import spring.springserver.domain.profile.entity.Profile
import java.time.LocalDateTime

data class ProfileResponse(
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

    val updatedAt: LocalDateTime?
) {

    companion object {

        fun of(
            profile: Profile,
            username: String,
            email: String,
            phone: String?,
            locationName: String?,
            jobCategoryName: String?,
            phoneVerified: Boolean,
            posts: List<PostResponse>
        ): ProfileResponse {

            return ProfileResponse(
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
                profile.getUpdatedAt()
            )
        }
    }
}
