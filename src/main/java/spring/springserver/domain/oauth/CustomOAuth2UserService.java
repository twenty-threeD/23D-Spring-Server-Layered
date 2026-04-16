package spring.springserver.domain.oauth;

import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // 구글마다 속성 키값이 다를 수 있어 보통 추상화하지만, 일단 구글 기준으로 작성
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");

        // 유저 저장 및 업데이트 로직
        Member member = memberRepository.findByEmail(email)
                .map(entity -> entity.update(name))
                .orElseGet(() -> Member.builder() // new Member(...) 대신 빌더 사용
                        .name(name)
                        .email(email)
                        .role(Role.USER)
                        .username(email)
                        .build());

        memberRepository.save(member);

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + member.getRole().name())),
                attributes,
                "email" // 구글의 기본 식별자 값
        );
    }
}
