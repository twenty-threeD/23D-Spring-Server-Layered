package spring.springserver.domain.auth.service.oauth

import org.springframework.context.ApplicationEventPublisher
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.auth.event.SocialMemberSignedUpEvent
import spring.springserver.domain.auth.exception.AuthStatusCode
import spring.springserver.domain.key.service.KeyService
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.member.entity.Provider
import spring.springserver.domain.member.entity.Role
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.domain.profile.service.ProfileService
import spring.springserver.global.exception.exception.ApplicationException

/**
 * @author @gnlandkmg(개발)
 * @author @L98293(코틀린 변환)
 */
@Service
class CustomOAuthUserService(
    private val memberRepository: MemberRepository,
    private val keyService: KeyService,
    private val profileService: ProfileService,
    private val eventPublisher: ApplicationEventPublisher,
    private val jdbcTemplate: JdbcTemplate
): DefaultOAuth2UserService() {

    companion object {

        /**
         * advisory lock의 첫 번째 키. 다른 기능의 잠금과 키가 겹치지 않게 구분한다.
         */
        private const val SOCIAL_LOGIN_LOCK_NAMESPACE = 2302
    }

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
        val providerId: String
        val emailVerified: Boolean

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
                providerId = attributes.getRequiredId("id")

                /**
                 * 카카오는 인증되지 않았거나 더 이상 유효하지 않은 이메일도 내려준다.
                 */
                emailVerified = kakaoAccount["is_email_verified"] == true
                        && kakaoAccount["is_email_valid"] == true
            }

            Provider.GOOGLE -> {

                email = attributes.getRequiredEmail()

                name = attributes.getName(
                    "name",
                    email
                )

                nameAttributeKey = "email"
                providerId = attributes.getRequiredId("sub")
                emailVerified = attributes["email_verified"] == true
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
                providerId = naverResponse.getRequiredId("id")

                /**
                 * 네이버는 인증 여부 값을 주지 않고, 내려주는 이메일은 네이버 계정에 등록된 연락처 이메일이다.
                 * 네이버가 이 이메일의 소유를 확인한다고 가정하고 인증된 것으로 본다.
                 */
                emailVerified = true
            }

            else -> throw ApplicationException(AuthStatusCode.UNKNOWN_REGISTRATION_ID)
        }

        lockSocialAccount(
            provider = provider,
            providerId = providerId,
            email = email
        )

        val existingMember = findExistingMember(
            provider = provider,
            providerId = providerId,
            email = email,
            emailVerified = emailVerified
        )
            ?.apply { update(name) }

        val member = memberRepository.save(
            existingMember
                ?: Member(
                    username = resolveUsername(email),
                    name = name,
                    email = email,
                    phone = null,
                    password = null,
                    role = Role.USER,
                    provider = provider
                ).apply { this.providerId = providerId }
        )

        if (existingMember == null) {

            keyService.generateKeyPair(
                memberId = member.getId()!!
            )

            profileService.createDefaultProfile(member)

            eventPublisher.publishEvent(SocialMemberSignedUpEvent(member.username))
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

    /**
     * 같은 소셜 계정의 첫 로그인이 동시에 들어오면 둘 다 회원이 없다고 보고 가입을 시도해
     * 늦은 쪽이 (provider, provider_id)·email 유니크 제약에 걸려 500이 난다.
     * 아직 없는 행은 행 잠금으로 막을 수 없어 고유 id와 이메일 단위로 PostgreSQL advisory lock을 잡는다.
     * 늦은 요청은 앞선 가입이 커밋될 때까지 기다린 뒤 그 회원을 찾아 로그인을 이어간다.
     * 교착을 피하려고 키를 정렬해서 잡고, 트랜잭션이 끝나면 자동으로 풀린다.
     */
    private fun lockSocialAccount(
        provider: Provider,
        providerId: String,
        email: String
    ) {

        listOf(
            "provider:${provider.name}:$providerId",
            "email:${email.trim().lowercase()}"
        )
            .sorted()
            .forEach { lockKey ->

                jdbcTemplate.queryForList(
                    "select pg_advisory_xact_lock(?, hashtext(?))",
                    SOCIAL_LOGIN_LOCK_NAMESPACE,
                    lockKey
                )
            }
    }

    /**
     * 소셜 회원의 username은 이메일을 그대로 쓰지만, username에는 형식 제한이 없어
     * 자체 가입자가 남의 이메일 문자열을 username으로 선점할 수 있다.
     * 그 경우 unique 제약에 걸려 가입 자체가 실패하므로 뒤에 번호를 붙여 비켜간다.
     */
    private fun resolveUsername(
        email: String
    ): String {

        if (!memberRepository.existsByUsername(email)) {

            return email
        }

        var suffix = 1

        while (memberRepository.existsByUsername("$email-$suffix")) {

            suffix++
        }

        return "$email-$suffix"
    }

    /**
     * 기존 회원은 공급자 고유 id로 찾는다. 이메일은 공급자 쪽에서 바뀌거나 다른 계정에 재사용될 수 있다.
     *
     * 고유 id로 못 찾았을 때만 이메일로 찾는데, 이때는 공급자가 인증한 이메일이어야 한다.
     * 인증되지 않은 이메일로 기존 계정에 연결되면 남의 계정으로 로그인하게 된다.
     * 이메일로 찾은 계정은 같은 가입 경로이고 아직 고유 id가 없는 경우(이 컬럼 도입 전 가입자)에만 연결한다.
     * 고유 id로 찾은 회원은 공급자 쪽 이메일이 바뀌어도 저장된 이메일을 갱신하지 않는다.
     */
    private fun findExistingMember(
        provider: Provider,
        providerId: String,
        email: String,
        emailVerified: Boolean
    ): Member? {

        memberRepository.findByProviderAndProviderId(
            provider,
            providerId
        )?.let { member ->

            /**
             * 탈퇴 회원도 행이 남아 있어 이 검사가 없으면 소셜 로그인으로 계정이 되살아난다.
             */
            if (member.isDeleted()) {

                throw ApplicationException(AuthStatusCode.WITHDRAWN_ACCOUNT)
            }

            return member
        }

        if (!emailVerified) {

            throw ApplicationException(AuthStatusCode.OAUTH_EMAIL_NOT_VERIFIED)
        }

        val member = memberRepository.findByEmail(email)
            ?: return null

        if (member.isDeleted()) {

            throw ApplicationException(AuthStatusCode.WITHDRAWN_ACCOUNT)
        }

        /**
         * 같은 이메일로 이미 가입된 계정이 있으면 가입 경로가 같을 때만 연결한다.
         * 이 검사가 없으면 자체 가입(AUTH) 계정이나 다른 소셜로 만든 계정을
         * 같은 이메일을 가진 소셜 계정으로 그대로 열어버린다.
         */
        if (member.provider != provider) {

            throw ApplicationException(AuthStatusCode.OAUTH_PROVIDER_MISMATCH)
        }

        /**
         * 같은 공급자인데 다른 고유 id가 이미 연결돼 있으면, 공급자 쪽에서 이메일이 다른 계정으로 넘어간 경우다.
         */
        if (member.providerId != null) {

            throw ApplicationException(AuthStatusCode.OAUTH_ACCOUNT_CONFLICT)
        }

        member.providerId = providerId

        return member
    }

    private fun Map<*, *>.getRequiredId(
        key: String
    ): String {

        return this[key]?.toString()?.takeIf { it.isNotBlank() }
            ?: throw ApplicationException(AuthStatusCode.UNKNOWN_REGISTRATION_ID)
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
