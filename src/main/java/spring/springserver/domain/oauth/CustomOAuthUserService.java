package spring.springserver.domain.oauth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import spring.springserver.domain.member.entity.Member;
import spring.springserver.domain.member.entity.Role;
import spring.springserver.domain.member.repository.MemberRepository;

import java.util.Collections;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomOAuthUserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);

        Map<String,Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");


        Member member = memberRepository.findByEmail(email)
                .map(entity -> entity.update(name))
                .orElse(Member.builder()
                        .name(name)
                        .email(email)
                        .role(Role.USER) // Role Enum 사용하신 부분 아주 좋습니다!
                        .provider("google")
                        .build());

        memberRepository.save(member); // 이제 정상적으로 Member 객체가 저장됩니다.

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(member.getRole().toString())),
                attributes,
                "email"
        );
    }
}
