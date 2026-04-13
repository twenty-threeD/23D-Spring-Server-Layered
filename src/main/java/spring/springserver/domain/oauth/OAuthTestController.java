package spring.springserver.domain.oauth;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OAuthTestController {

    @GetMapping(value = "/", produces = "text/html; charset=UTF-8")
    public String home(@AuthenticationPrincipal OAuth2User oauth2User) {
        if (oauth2User != null) {
            String name = oauth2User.getAttribute("name");
            String email = oauth2User.getAttribute("email");
            return "구글 로그인 성공 <br>이름: " + name + "<br>이메일: " + email;
        }
        // a 태그가 제대로 작동하도록 HTML로 반환됩니다.
        return "로그인 전입니다. <br><a href='/oauth2/authorization/google'>구글 로그인하기</a>";
    }
}
