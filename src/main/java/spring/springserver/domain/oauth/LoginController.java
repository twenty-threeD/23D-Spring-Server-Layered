package spring.springserver.domain.oauth;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class LoginController {

    @GetMapping("/api/oauth/signin/success")
    public Map<String,Object> getProfile(@AuthenticationPrincipal OAuth2User oAuth2User) {

        if (oAuth2User == null) {
            return Map.of("Message", "로그인된 사용자가 없습니다.");
        }

        return oAuth2User.getAttributes();
    }
}
