package spring.springserver.domain.profile.data.response

import spring.springserver.domain.profile.entity.Profile
import java.time.LocalDateTime

data class ProfileResponse(
    val username: String,

    val imageUrl: String?,

    val sigCd: String?,

    val locationName: String?,

    val movableDistance: String?,

    val movableDistanceLabel: String?,

    val shortDescription: String?,

    val jobCategoryId: Long?,

    val jobCategoryName: String?,

    val updatedAt: LocalDateTime?
) {

    companion object {

        fun of(
            profile: Profile,
            username: String,
            locationName: String?,
            jobCategoryName: String?
        ): ProfileResponse {

            return ProfileResponse(
                username,
                profile.imageUrl,
                profile.sig?.getSigCd(),
                locationName,
                profile.movableDistance?.name,
                profile.movableDistance?.getLabel(),
                profile.shortDescription,
                profile.jobCategory?.getId(),
                jobCategoryName,
                profile.getUpdatedAt()
            )
        }
    }
}