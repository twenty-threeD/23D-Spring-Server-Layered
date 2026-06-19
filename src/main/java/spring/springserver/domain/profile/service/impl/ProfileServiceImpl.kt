package spring.springserver.domain.profile.service.impl

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.auth.exception.AuthStatusCode
import spring.springserver.domain.auth.service.token.TokenService
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

@Service
@Transactional(rollbackFor = [Exception::class])
class ProfileServiceImpl(
    private val profileRepository: ProfileRepository,
    private val memberRepository: MemberRepository,
    private val locationService: LocationService,
    private val jobCategoryService: JobCategoryService,
    private val tokenService: TokenService
): ProfileService {

    override fun createDefaultProfile(
        member: Member
    ) {

        if (profileRepository.existsByMember(member)) {

            throw ApplicationException(ProfileStatusCode.PROFILE_ALREADY_EXIST)
        }

        profileRepository.save(Profile(member = member))
    }

    @Transactional(readOnly = true)
    override fun getMyProfile(): ProfileResponse {

        val member = getCurrentMember()

        return toResponse(getCurrentProfile(member), member)
    }

    override fun updateMyProfile(
        updateProfileRequest: UpdateProfileRequest,
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ): UpdateProfileResponse {

        val member = getCurrentMember()

        val profile = getCurrentProfile(member)

        val usernameChanged = applyUsername(member, updateProfileRequest.username?.trim())

        profile.update(
            imageUrl = updateProfileRequest.imageUrl?.trim()?.takeIf { it.isNotBlank() }
                ?: profile.imageUrl,
            sig = resolveSig(updateProfileRequest.sigCd, profile.sig),
            movableDistance = updateProfileRequest.movableDistance
                ?: profile.movableDistance,
            shortDescription = updateProfileRequest.shortDescription?.trim()?.takeIf { it.isNotBlank() }
                ?: profile.shortDescription,
            jobCategory = resolveJobCategory(updateProfileRequest.jobCategoryId, profile.jobCategory)
        )

        if (usernameChanged) {

            tokenService.deleteTokens(httpServletRequest, httpServletResponse)

            return UpdateProfileResponse.of("프로필이 수정되었습니다. 사용자명이 변경되어 다시 로그인 해주세요.")
        }

        return UpdateProfileResponse.of("프로필이 수정되었습니다.")
    }

    private fun applyUsername(member: Member,
                              username: String?): Boolean {

        if (username == null || username == member.username) {

            return false
        }

        if (memberRepository.existsByUsername(username)) {

            throw ApplicationException(AuthStatusCode.USERNAME_ALREADY_EXIST)
        }

        member.username = username

        return true
    }

    private fun resolveSig(sigCd: String?,
                           current: Sig?): Sig? {

        if (sigCd.isNullOrBlank()) {

            return current
        }

        return locationService.getSig(sigCd.trim())
    }

    private fun resolveJobCategory(jobCategoryId: Long?,
                                   current: JobCategory?): JobCategory? {

        if (jobCategoryId == null) {

            return current
        }

        return jobCategoryService.getJobCategory(jobCategoryId)
    }

    private fun toResponse(profile: Profile,
                           member: Member): ProfileResponse {

        return ProfileResponse.of(
            profile = profile,
            username = member.username,
            locationName = profile.sig?.let { locationService.getFullName(it) },
            jobCategoryName = profile.jobCategory?.getFullName()
        )
    }

    private fun getCurrentProfile(
        member: Member
    ): Profile {

        return profileRepository.findByMember(member)
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