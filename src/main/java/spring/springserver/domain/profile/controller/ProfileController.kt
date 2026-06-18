package spring.springserver.domain.profile.controller

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import spring.springserver.domain.profile.data.request.UpdateProfileRequest
import spring.springserver.domain.profile.data.response.ProfileResponse
import spring.springserver.domain.profile.data.response.UpdateProfileResponse
import spring.springserver.domain.profile.service.ProfileService
import spring.springserver.global.data.BaseResponse

@RestController
@RequestMapping("/api/profile")
class ProfileController(
    private val profileService: ProfileService
) {

    @GetMapping
    fun getMyProfile(): BaseResponse<ProfileResponse> {

        return BaseResponse.ok(profileService.getMyProfile())
    }

    @PatchMapping
    fun updateMyProfile(@Valid @RequestBody updateProfileRequest: UpdateProfileRequest): BaseResponse<UpdateProfileResponse> {

        return BaseResponse.ok(profileService.updateMyProfile(updateProfileRequest))
    }
}