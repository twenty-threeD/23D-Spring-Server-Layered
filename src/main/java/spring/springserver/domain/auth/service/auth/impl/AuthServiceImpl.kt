package spring.springserver.domain.auth.service.auth.impl

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.auth.data.request.GenerateTokenRequest
import spring.springserver.domain.auth.data.request.OAuthExchangeRequest
import spring.springserver.domain.auth.data.request.SignInRequest
import spring.springserver.domain.auth.data.request.SignUpRequest
import spring.springserver.domain.auth.data.response.SignInResponse
import spring.springserver.domain.auth.data.response.SignOutResponse
import spring.springserver.domain.auth.data.response.SignUpResponse
import spring.springserver.domain.auth.exception.AuthStatusCode
import spring.springserver.domain.auth.service.auth.AuthService
import spring.springserver.domain.auth.service.token.TokenService
import spring.springserver.domain.key.service.KeyService
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.domain.member.retention.MemberRetentionService
import spring.springserver.domain.phone.service.PhoneVerifyService
import spring.springserver.domain.profile.service.ProfileService
import spring.springserver.global.exception.exception.ApplicationException
import spring.springserver.global.util.PhoneNormalizer
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import java.util.concurrent.TimeUnit

@Service
@Transactional(rollbackFor = [Exception::class])
class AuthServiceImpl(
    private val passwordEncoder: PasswordEncoder,
    private val memberRepository: MemberRepository,
    private val tokenService: TokenService,
    private val keyService: KeyService,
    private val profileService: ProfileService,
    private val phoneVerifyService: PhoneVerifyService,
    private val memberRetentionService: MemberRetentionService,
    private val redisTemplate: RedisTemplate<String, String>
): AuthService {

    /**
     * 회원이 없을 때도 같은 비용의 BCrypt 비교를 하기 위한 해시.
     * 비교를 건너뛰면 응답 시간 차이로 가입된 이메일인지 드러난다.
     */
    private val dummyPasswordHash by lazy { passwordEncoder.encode(UUID.randomUUID().toString()) }

    companion object {

        private const val OAUTH_EXCHANGE_CODE_PREFIX = "oauthExchangeCode:"

        private const val OAUTH_EXCHANGE_CODE_TTL_SECONDS = 60L

        private const val SIGN_IN_FAILURE_PREFIX = "signIn:failure:"

        private const val MAX_SIGN_IN_FAILURES = 5L

        private const val SIGN_IN_LOCK_MINUTES = 15L

        private val secureRandom = SecureRandom()
    }

    override fun signUp(
        signUpRequest: SignUpRequest
    ): SignUpResponse {

        val phone = PhoneNormalizer.normalize(signUpRequest.phone)

        /**
         * 탈퇴 회원도 행이 남고 username·email·phone의 unique 제약이 그대로라 재가입이 막힌다.
         * 제한 기간(MemberRetentionService.RETENTION_DAYS)이 지났으면 값을 풀어 가입시키고,
         * 기간 중이면 "이미 존재"와 구분되는 코드로 탈퇴 계정임을 알려준다.
         */
        checkDuplicate(
            member = memberRepository.findByUsername(signUpRequest.username),
            duplicateStatusCode = AuthStatusCode.USERNAME_ALREADY_EXIST
        )

        checkDuplicate(
            member = memberRepository.findByEmail(signUpRequest.email),
            duplicateStatusCode = AuthStatusCode.EMAIL_ALREADY_EXIST
        )

        if (phone != null) {

            checkDuplicate(
                member = memberRepository.findByPhone(phone),
                duplicateStatusCode = AuthStatusCode.PHONE_ALREADY_EXIST
            )
        }

        val newMember = signUpRequest.toEntity(
            encodedPassword = passwordEncoder.encode(signUpRequest.password),
            normalizedPhone = phone
        )

        /**
         * 가입 직전에 로그인 없이 본인인증을 마쳤다면 그 표식을 소비해 인증 상태로 가입시킨다.
         * 건너뛰었다면 미인증 상태로 가입되고, 나중에 로그인 후 인증하면 된다.
         */
        if (phone != null && phoneVerifyService.consumePhoneVerification(phone = phone)) {

            newMember.verifyPhone(phone = phone)
        }

        val member = memberRepository.save(newMember)

        keyService.generateKeyPair(
            memberId = member.getId()!!
        )

        profileService.createDefaultProfile(member)

        return SignUpResponse.of("회원가입이 완료 되었습니다.")
    }

    override fun signIn(
        signInRequest: SignInRequest,
        httpServletResponse: HttpServletResponse
    ): SignInResponse {

        val failureKey = SIGN_IN_FAILURE_PREFIX + signInRequest.email.trim().lowercase()

        /**
         * 비밀번호 무차별 대입을 막기 위해 이메일마다 시도 횟수를 센다.
         * 비교보다 먼저 횟수를 올려야 동시에 몰린 요청도 한도 이상 비교되지 않는다.
         * 한도를 넘으면 비밀번호가 맞아도 잠금이 풀릴 때까지 거부하고, 로그인에 성공하면 초기화한다.
         */
        val attempts = redisTemplate.opsForValue().increment(failureKey)
            ?: throw ApplicationException(AuthStatusCode.INVALID_CREDENTIALS)

        if (attempts == 1L) {

            redisTemplate.expire(
                failureKey,
                SIGN_IN_LOCK_MINUTES,
                TimeUnit.MINUTES
            )
        }

        if (attempts > MAX_SIGN_IN_FAILURES) {

            throw ApplicationException(AuthStatusCode.SIGN_IN_ATTEMPTS_EXCEEDED)
        }

        val member = memberRepository.findByEmail(signInRequest.email)

        /**
         * 회원이 없거나 비밀번호가 없는 소셜 가입자여도 BCrypt 비교를 한 번 해서 응답 시간을 맞춘다.
         * 이때 더미 해시와 일치해도 로그인되면 안 되므로, 저장된 비밀번호가 있을 때만 일치로 본다.
         * 탈퇴한 회원은 행이 남아 있으므로 로그인 단계에서 걸러내고,
         * 탈퇴 여부를 알려주지 않기 위해 자격 증명 오류와 같은 응답을 준다.
         */
        val encodedPassword = member?.password?.takeIf { it.isNotBlank() }

        val passwordMatches = passwordEncoder.matches(
            signInRequest.password,
            encodedPassword ?: dummyPasswordHash
        ) && encodedPassword != null

        if (member == null || member.isDeleted() || !passwordMatches) {

            throw ApplicationException(AuthStatusCode.INVALID_CREDENTIALS)
        }

        redisTemplate.delete(failureKey)

        val generateTokenRequest = GenerateTokenRequest(
            member.username,
            member.role
        )

        return SignInResponse.of(
            tokenService.generateAccessToken(
                generateTokenRequest,
                httpServletResponse
            ),
            tokenService.generateRefreshToken(
                generateTokenRequest,
                httpServletResponse
            )
        )
    }

    override fun signOut(
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ): SignOutResponse {

        tokenService.deleteTokens(
            httpServletRequest,
            httpServletResponse
        )

        return SignOutResponse.of("로그아웃 되었습니다.")
    }

    /**
     * 가입에 쓰려는 값이 이미 쓰이고 있으면 예외를 던진다.
     * 그 값을 쥐고 있는 회원이 탈퇴 회원이면 재가입 불가임을 구분해 알려준다.
     */
    private fun checkDuplicate(
        member: Member?,
        duplicateStatusCode: AuthStatusCode
    ) {

        if (member == null) {

            return
        }

        if (member.isDeleted()) {

            /**
             * 재가입 제한 기간이 지났으면 그 자리에서 값을 풀어주고 가입을 통과시킨다.
             */
            if (memberRetentionService.releaseIfRetentionExpired(member.getId()!!)) {

                return
            }

            throw ApplicationException(AuthStatusCode.WITHDRAWN_ACCOUNT_CANNOT_REJOIN)
        }

        throw ApplicationException(duplicateStatusCode)
    }

    override fun issueOAuthExchangeCode(
        generateTokenRequest: GenerateTokenRequest
    ): String {

        val bytes = ByteArray(32)

        secureRandom.nextBytes(bytes)

        val code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

        redisTemplate.opsForValue().set(
            OAUTH_EXCHANGE_CODE_PREFIX + code,
            generateTokenRequest.username,
            OAUTH_EXCHANGE_CODE_TTL_SECONDS,
            TimeUnit.SECONDS
        )

        return code
    }

    /**
     * 코드는 꺼내는 즉시 지워 한 번만 쓸 수 있다.
     * 역할은 코드를 만든 시점이 아니라 교환 시점의 회원 정보로 다시 읽는다.
     */
    override fun exchangeOAuthCode(
        oAuthExchangeRequest: OAuthExchangeRequest,
        httpServletResponse: HttpServletResponse
    ): SignInResponse {

        val username = redisTemplate.opsForValue().getAndDelete(OAUTH_EXCHANGE_CODE_PREFIX + oAuthExchangeRequest.code)
            ?: throw ApplicationException(AuthStatusCode.INVALID_OAUTH_EXCHANGE_CODE)

        val member = memberRepository.findByUsername(username)
            ?: throw ApplicationException(AuthStatusCode.INVALID_OAUTH_EXCHANGE_CODE)

        if (member.isDeleted()) {

            throw ApplicationException(AuthStatusCode.INVALID_OAUTH_EXCHANGE_CODE)
        }

        val generateTokenRequest = GenerateTokenRequest(
            member.username,
            member.role
        )

        return SignInResponse.of(
            tokenService.generateAccessToken(
                generateTokenRequest,
                httpServletResponse
            ),
            tokenService.generateRefreshToken(
                generateTokenRequest,
                httpServletResponse
            )
        )
    }

    override fun verifyPassword(
        httpServletRequest: HttpServletRequest,
        rawPassword: String
    ): Boolean {

        return passwordEncoder.matches(rawPassword,
            memberRepository.findByUsername(
                tokenService.getCurrentUsername(httpServletRequest)
            )?.password
        )
    }
}