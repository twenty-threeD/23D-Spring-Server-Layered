package spring.springserver.domain.oauth.service

import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import spring.springserver.domain.auth.exception.AuthStatusCode
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.member.entity.Provider
import spring.springserver.domain.member.entity.Role
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.global.exception.exception.ApplicationException

/**
 * @author @gnlandkmg(개발)
 * @author @L98293(코틀린 변환)
 */
@Service
class CustomOAuthUserService(private val memberRepository: MemberRepository): DefaultOAuth2UserService() {

    override fun loadUser(oAuth2UserRequest: OAuth2UserRequest): OAuth2User {

        val oAuth2User = super.loadUser(oAuth2UserRequest)
        val attributes = oAuth2User.attributes
        val provider = Provider.getRegistrationId(oAuth2UserRequest.clientRegistration.registrationId)

        val customAttributes = attributes.toMutableMap()

        val email: String
        val name: String
        val nameAttributeKey: String

        when (provider) {

            Provider.KAKAO -> {

                val kakaoAccount = attributes["kakao_account"] as? Map<*, *>
                    ?: throw ApplicationException(AuthStatusCode.UNKNOWN_REGISTRATION_ID)

                email = kakaoAccount["email"].toString()

                val profile = kakaoAccount["profile"] as? Map<*, *>
                    ?: throw ApplicationException(AuthStatusCode.UNKNOWN_REGISTRATION_ID)

                name = profile["nickname"].toString()

                nameAttributeKey = "id"
            }

            Provider.GOOGLE -> {

                email = attributes["email"].toString()

                name = attributes["name"].toString()

                nameAttributeKey = "email"
            }

            else -> throw ApplicationException(AuthStatusCode.UNKNOWN_REGISTRATION_ID)
        }

        val member = memberRepository.findByEmail(email)
            ?.apply { update(name) }
            ?: Member(
                username = email,
                name = name,
                email = email,
                phone = null,
                password = null,
                role = Role.USER,
                provider = provider
            )

        memberRepository.save(member)

        customAttributes["username"] = member.username
        customAttributes["role"] = member.role.name
        customAttributes["provider"] = member.provider.name

        return DefaultOAuth2User(
            setOf(SimpleGrantedAuthority("ROLE_${member.role.name}")),
            customAttributes,
            nameAttributeKey
        )
    }
}