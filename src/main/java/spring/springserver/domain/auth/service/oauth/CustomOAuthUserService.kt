package spring.springserver.domain.auth.service.oauth

import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.auth.exception.AuthStatusCode
import spring.springserver.domain.key.service.KeyService
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.member.entity.Provider
import spring.springserver.domain.member.entity.Role
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.domain.profile.service.ProfileService
import spring.springserver.global.exception.exception.ApplicationException
import kotlin.collections.get

/**
 * @author @gnlandkmg(개발)
 * @author @L98293(코틀린 변환)
 */
@Service
class CustomOAuthUserService(
    private val memberRepository: MemberRepository,
    private val keyService: KeyService,
    private val profileService: ProfileService
): DefaultOAuth2UserService() {

    @Transactional(rollbackFor = [Exception::class])
    override fun loadUser(oAuth2UserRequest: OAuth2UserRequest): OAuth2User {

        try {

            return loadMember(oAuth2UserRequest)
        } catch (applicationException: ApplicationException) {

            throw OAuth2AuthenticationException(
                OAuth2Error(
                    applicationException.statusCode.getCode(),
                    applicationException.statusCode.getMessage(),
                    null
                ),
                applicationException
            )
        }
    }

    private fun loadMember(
        oAuth2UserRequest: OAuth2UserRequest
    ): OAuth2User {

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

                email = kakaoAccount.getRequiredEmail()

                val profile = kakaoAccount["profile"] as? Map<*, *>
                    ?: throw ApplicationException(AuthStatusCode.UNKNOWN_REGISTRATION_ID)

                name = profile.getName(
                    "nickname",
                    email
                )

                nameAttributeKey = "id"
            }

            Provider.GOOGLE -> {

                email = attributes.getRequiredEmail()

                name = attributes.getName(
                    "name",
                    email
                )

                nameAttributeKey = "email"
            }

            Provider.NAVER -> {

                val naverResponse = attributes["response"] as? Map<*, *>
                    ?: throw ApplicationException(AuthStatusCode.UNKNOWN_REGISTRATION_ID)

                email = naverResponse.getRequiredEmail()

                name = naverResponse.getName(
                    "name",
                    email
                )

                customAttributes["id"] = naverResponse["id"]
                    ?: throw ApplicationException(AuthStatusCode.UNKNOWN_REGISTRATION_ID)

                nameAttributeKey = "id"
            }

            else -> throw ApplicationException(AuthStatusCode.UNKNOWN_REGISTRATION_ID)
        }

        val existingMember = memberRepository.findByEmail(email)?.apply { update(name) }

        val member = memberRepository.save(
            existingMember
                ?: Member(
                    username = email,
                    name = name,
                    email = email,
                    phone = null,
                    password = null,
                    role = Role.USER,
                    provider = provider
                )
        )

        if (existingMember == null) {

            keyService.generateKeyPair(
                memberId = member.getId()!!
            )

            profileService.createDefaultProfile(member)
        }

        customAttributes["username"] = member.username
        customAttributes["role"] = member.role.name
        customAttributes["provider"] = member.provider.name

        return DefaultOAuth2User(
            setOf(SimpleGrantedAuthority("ROLE_${member.role.name}")),
            customAttributes,
            nameAttributeKey
        )
    }

    private fun Map<*, *>.getRequiredEmail(): String {

        return this["email"]?.toString()?.takeIf { it.isNotBlank() }
            ?: throw ApplicationException(AuthStatusCode.OAUTH_EMAIL_NOT_PROVIDED)
    }

    private fun Map<*, *>.getName(
        key: String,
        email: String
    ): String {

        return this[key]?.toString()?.takeIf { it.isNotBlank() }
            ?: email.substringBefore("@")
    }
}
