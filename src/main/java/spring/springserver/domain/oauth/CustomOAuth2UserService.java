package spring.springserver.domain.oauth;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import spring.springserver.domain.auth.data.request.GenerateTokenRequest;
import spring.springserver.domain.auth.service.TokenService;
import spring.springserver.domain.member.entity.Member;
import spring.springserver.domain.member.repository.MemberRepository;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;
    private final TokenService tokenService;

    public OAuthResponse loadUser(OAuth2UserRequest oAuth2UserRequest,
                               OAuthRequest oAuthRequest,
                               HttpServletResponse httpServletResponse) throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        Member member = memberRepository.findByEmail(oAuthRequest.email());

        member.updateName(name);

        memberRepository.save(member);

        GenerateTokenRequest generateTokenRequest = new GenerateTokenRequest (
                member.getUsername(),
                member.getRole()
        );

        new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + member.getRole().name())),
                oAuth2User.getAttributes(),
                "email"
        );

        return OAuthResponse.of(
                tokenService.generateAccessToken(
                        generateTokenRequest,
                        httpServletResponse
                ),
                tokenService.generateRefreshToken(
                        generateTokenRequest,
                        httpServletResponse
                )
        );

    }
}
