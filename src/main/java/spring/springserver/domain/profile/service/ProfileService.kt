package spring.springserver.domain.profile.service

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.profile.data.request.UpdateProfileRequest
import spring.springserver.domain.profile.data.response.ProfileResponse
import spring.springserver.domain.profile.data.response.UpdateProfileResponse

interface ProfileService {

    fun createDefaultProfile(member: Member)

    fun getMyProfile(): ProfileResponse

    fun updateMyProfile(updateProfileRequest: UpdateProfileRequest,
                        httpServletRequest: HttpServletRequest,
                        httpServletResponse: HttpServletResponse): UpdateProfileResponse
}