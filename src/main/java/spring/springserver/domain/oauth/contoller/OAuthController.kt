package spring.springserver.domain.oauth.contoller

import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import spring.springserver.domain.oauth.data.response.OAuthResponse
import spring.springserver.global.data.BaseResponse

/**
 * @author @gnlandkmg(개발)
 * @author @L98293(코틀린 변환)
 */
@RestController
@RequestMapping("/api/oauth")
class OAuthController {

    @GetMapping("/signin/success")
    fun getProfile(@Valid @AuthenticationPrincipal oAuth2User: OAuth2User): BaseResponse<OAuthResponse> {

        val email = oAuth2User.attributes["email"].toString()
        val name = oAuth2User.attributes["name"].toString()

        return BaseResponse.ok(
            OAuthResponse(
                email,
                name
            )
        )
    }
}