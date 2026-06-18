package spring.springserver.domain.profile.service.impl

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.auth.exception.AuthStatusCode
import spring.springserver.domain.jobcategory.entity.JobCategory
import spring.springserver.domain.jobcategory.service.JobCategoryService
import spring.springserver.domain.location.entity.Sig
import spring.springserver.domain.location.service.LocationService
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.domain.profile.data.request.UpdateProfileRequest
import spring.springserver.domain.profile.data.response.ProfileResponse
import spring.springserver.domain.profile.data.response.UpdateProfileResponse
import spring.springserver.domain.profile.entity.Profile
import spring.springserver.domain.profile.exception.ProfileStatusCode
import spring.springserver.domain.profile.repository.ProfileRepository
import spring.springserver.domain.profile.service.ProfileService
import spring.springserver.global.exception.exception.ApplicationException
import java.time.LocalDateTime

@Service
@Transactional(rollbackFor = [Exception::class])
class ProfileServiceImpl(
    private val profileRepository: ProfileRepository,
    private val memberRepository: MemberRepository,
    private val locationService: LocationService,
    private val jobCategoryService: JobCategoryService
): ProfileService {

    companion object {

        private const val NICKNAME_CHANGE_COOLDOWN_DAYS = 30L
    }

    override fun createDefaultProfile(
        member: Member
    ) {

        if (profileRepository.existsByMember(member)) {

            throw ApplicationException(ProfileStatusCode.PROFILE_ALREADY_EXIST)
        }

        profileRepository.save(
            Profile(
                member = member,
                nickname = member.username
            )
        )
    }

    @Transactional(readOnly = true)
    override fun getMyProfile(): ProfileResponse {

        val profile = getCurrentProfile()

        return toResponse(profile)
    }

    override fun updateMyProfile(
        updateProfileRequest: UpdateProfileRequest
    ): UpdateProfileResponse {

        val profile = getCurrentProfile()

        applyNickname(
            profile,
            updateProfileRequest.nickname.trim()
        )

        profile.update(
            imageUrl = updateProfileRequest.imageUrl?.trim()?.takeIf { it.isNotBlank() },
            sig = resolveSig(updateProfileRequest.sigCd),
            movableDistance = updateProfileRequest.movableDistance,
            shortDescription = updateProfileRequest.shortDescription?.trim()?.takeIf { it.isNotBlank() },
            jobCategory = resolveJobCategory(updateProfileRequest.jobCategoryId)
        )

        return UpdateProfileResponse.of("프로필이 수정되었습니다.")
    }

    private fun applyNickname(profile: Profile,
                              nickname: String) {

        if (profile.nickname == nickname) {

            return
        }

        if (LocalDateTime.now()
                .isBefore(profile.getNicknameUpdatedAt().plusDays(NICKNAME_CHANGE_COOLDOWN_DAYS))) {

            throw ApplicationException(ProfileStatusCode.NICKNAME_CHANGE_NOT_ALLOWED)
        }

        profile.updateNickname(nickname)
    }

    private fun resolveSig(
        sigCd: String?
    ): Sig? {

        return sigCd?.takeIf { it.isNotBlank() }
            ?.let { locationService.getSig(it) }
    }

    private fun resolveJobCategory(
        jobCategoryId: Long?
    ): JobCategory? {

        return jobCategoryId?.let { jobCategoryService.getJobCategory(it) }
    }

    private fun toResponse(
        profile: Profile
    ): ProfileResponse {

        return ProfileResponse.of(
            profile = profile,
            locationName = profile.sig?.let { locationService.getFullName(it) },
            jobCategoryName = profile.jobCategory?.getFullName()
        )
    }

    private fun getCurrentProfile(): Profile {

        return profileRepository.findByMember(getCurrentMember())
            ?: throw ApplicationException(ProfileStatusCode.PROFILE_NOT_FOUND)
    }

    private fun getCurrentMember(): Member {

        val username = SecurityContextHolder.getContext().authentication?.name

        if (username.isNullOrBlank() || username == "anonymousUser") {

            throw ApplicationException(AuthStatusCode.INVALID_JWT)
        }

        return memberRepository.findByUsername(username)
            ?: throw ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)
    }
}