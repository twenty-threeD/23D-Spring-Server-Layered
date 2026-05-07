package spring.springserver.domain.oauth.service

import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.member.entity.Role
import spring.springserver.domain.member.repository.MemberRepository

/**
 * @author @gnlandkmg(개발)
 * @author @L98293(코틀린 변환)
 */
@Service
class CustomOAuthUserService(private val memberRepository: MemberRepository): DefaultOAuth2UserService() {

    override fun loadUser(oAuth2UserRequest: OAuth2UserRequest): OAuth2User {

        val attributes = super.loadUser(oAuth2UserRequest).attributes

        val email = attributes["email"].toString()
        val name = attributes["name"].toString()

        val member = memberRepository.findByEmail(email)
            ?.apply { update(name) }
            ?: Member(
                username = email, // 사용자명이 없기에
                name = name,
                email = email,
                phone = null,
                password = null,
                role = Role.USER
            )

        memberRepository.save(member)

        return DefaultOAuth2User(
            setOf(SimpleGrantedAuthority("ROLE_${member.role.name}")),
            attributes,
            "email"
        )
    }
}