package spring.springserver.domain.oauth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.springserver.global.data.BaseResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/oauth")
public class OAuthController {

    private final CustomOAuth2UserService customOAuth2UserService;

    @GetMapping("/signin/success")
//    @GetMapping("/loginSuccess")
    public BaseResponse<OAuthResponse> getProfile(@Valid @AuthenticationPrincipal OAuth2User oAuth2User) {

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        OAuthResponse oauthResponse = new OAuthResponse(email, name);

        return BaseResponse.ok(oauthResponse);
    }
}
