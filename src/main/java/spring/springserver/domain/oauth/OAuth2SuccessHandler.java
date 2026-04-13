package spring.springserver.domain.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import spring.springserver.domain.auth.interfaces.TokenProvider;
import spring.springserver.global.jwt.JwtProvider;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;

    public OAuth2SuccessHandler(TokenProvider jwtProvider) {

        this.jwtProvider = (JwtProvider) jwtProvider;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest httpServletRequest,
                                        HttpServletResponse httpServletResponse,
                                        Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");

        String token = jwtProvider.generateRefreshToken(email);

        Cookie cookie = new Cookie("JWT_TOKEN", token);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(60*60); //쿠키 수명은 임시로 1시간으로
        httpServletResponse.addCookie(cookie);

        getRedirectStrategy().sendRedirect(httpServletRequest, httpServletResponse, "/");
    }

}
