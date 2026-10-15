package spring.springserver.domain.auth.service.token.impl

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.auth.data.request.GenerateTokenRequest
import spring.springserver.domain.auth.data.request.ReissueTokenRequest
import spring.springserver.domain.auth.data.response.ReissueTokenResponse
import spring.springserver.domain.auth.entity.RefreshToken
import spring.springserver.domain.auth.exception.AuthStatusCode
import spring.springserver.domain.auth.repository.RefreshTokenRepository
import spring.springserver.domain.auth.service.token.TokenService
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.global.exception.exception.ApplicationException
import spring.springserver.global.jwt.JwtProvider
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.HexFormat
import java.util.concurrent.TimeUnit

@Service
@Transactional(rollbackFor = [Exception::class])
class TokenServiceImpl(
    private val jwtProvider: JwtProvider,
    private val redisTemplate: RedisTemplate<String, String>,
    private val memberRepository: MemberRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jdbcTemplate: JdbcTemplate,
    @param:Value($$"${spring.jwt.accessTokenExpiration}") private val accessTokenExpiration: Long,
    @param:Value($$"${spring.jwt.refreshTokenExpiration}") private val refreshTokenExpiration: Long,
    @param:Value($$"${app.cookie.same-site}") private val cookieSameSite: String,
    @param:Value($$"${app.cookie.secure}") private val cookieSecure: Boolean,
    @param:Value($$"${app.cookie.domain}") private val cookieDomain: String
): TokenService {

    companion object {

        private const val REVOKED_ACCESS_TOKEN_PREFIX = "revokedAccessToken:"

        private const val TOKEN_VALID_AFTER_PREFIX = "tokenValidAfter:"

        /**
         * advisory lock 키의 상위 16비트. 다른 기능이 같은 회원 id로 잠금을 잡아도 겹치지 않게 구분한다.
         */
        private const val REFRESH_TOKEN_LOCK_NAMESPACE = 2303L shl 48
    }

    override fun generateAccessToken(
        generateTokenRequest: GenerateTokenRequest,
        httpServletResponse: HttpServletResponse?
    ): String {

        val accessToken = jwtProvider.generateAccessToken(generateTokenRequest)

        redisTemplate.opsForValue().set(
            "accessToken:${generateTokenRequest.username}",
            accessToken,
            accessTokenExpiration,
            TimeUnit.MILLISECONDS
        )

        addCookie(
            "accessToken",
            accessToken,
            toCookieMaxAge(accessTokenExpiration),
            true,
            httpServletResponse
        )

        return accessToken
    }

    override fun generateRefreshToken(getTokenRequest: GenerateTokenRequest,
                                      httpServletResponse: HttpServletResponse?
    ): String {

        val refreshToken = jwtProvider.generateRefreshToken(getTokenRequest)
            ?: throw ApplicationException(AuthStatusCode.INVALID_JWT)

        val member = memberRepository.findByUsername(getTokenRequest.username)
            ?: throw ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)

        val expiresAt = toExpiresAt(refreshTokenExpiration)

        /**
         * refreshToken은 회원당 한 행인데 member_id에 유니크 제약이 없어, 같은 회원의 로그인이 동시에 들어오면
         * 둘 다 행이 없다고 보고 각각 저장해 행이 두 개가 된다. 그 뒤에는 findByMemberId가 실패한다.
         * 아직 없는 행은 행 잠금으로 막을 수 없어 회원 단위 advisory lock으로 저장을 한 번에 하나씩 처리한다.
         */
        jdbcTemplate.queryForList(
            "select pg_advisory_xact_lock(?)",
            REFRESH_TOKEN_LOCK_NAMESPACE or member.getId()!!
        )

        val saved = refreshTokenRepository.findByMemberId(member.getId()!!)

        if (saved != null) {

            saved.update(
                refreshToken,
                expiresAt
            )
        } else {

            refreshTokenRepository.save(
                RefreshToken(
                    member = member,
                    token = refreshToken,
                    expiresAt = expiresAt
                )
            )
        }

        addCookie(
            "refreshToken",
            refreshToken,
            toCookieMaxAge(refreshTokenExpiration),
            true,
            httpServletResponse
        )

        return refreshToken
    }

    /**
     * accessToken이 만료돼도 refreshToken이 살아 있으면 accessToken만 새로 발급한다.
     * refreshToken은 회전시키지 않으므로 만료 시점까지 그대로 쓴다.
     */
    override fun reissueAccessToken(
        reissueTokenRequest: ReissueTokenRequest?,
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ): ReissueTokenResponse {

        val refreshToken = extractTokenFromCookie("refreshToken", httpServletRequest)
            ?: reissueTokenRequest?.refreshToken?.takeIf { it.isNotBlank() }
            ?: throw ApplicationException(AuthStatusCode.INVALID_JWT)

        if (jwtProvider.isNotValidToken(refreshToken)) {

            throw ApplicationException(AuthStatusCode.INVALID_JWT)
        }

        val saved = refreshTokenRepository.findByToken(refreshToken)
            ?: throw ApplicationException(AuthStatusCode.INVALID_JWT)

        if (saved.isExpired()) {

            throw ApplicationException(AuthStatusCode.INVALID_JWT)
        }

        val username = jwtProvider.getUsernameFromToken(refreshToken)
            ?: throw ApplicationException(AuthStatusCode.INVALID_JWT)

        val member = memberRepository.findByUsername(username)
            ?: throw ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)

        /**
         * 탈취된 토큰으로 남의 accessToken을 받아가지 못하도록
         * refreshToken의 주인과 저장된 행의 주인이 같은지 확인한다.
         */
        if (saved.getMember().getId() != member.getId()) {

            throw ApplicationException(AuthStatusCode.INVALID_JWT)
        }

        /**
         * 탈퇴 시 저장된 refreshToken을 지우지만, 그 처리가 어긋난 경우에도
         * 탈퇴 회원이 accessToken을 다시 받아가지 못하도록 여기서도 막는다.
         */
        if (member.isDeleted()) {

            throw ApplicationException(AuthStatusCode.INVALID_JWT)
        }

        val accessToken = generateAccessToken(
            GenerateTokenRequest(
                username = member.username,
                role = member.role
            ),
            httpServletResponse
        )

        return ReissueTokenResponse.of(accessToken)
    }

    override fun deleteTokens(httpServletRequest: HttpServletRequest,
                              httpServletResponse: HttpServletResponse) {

        val accessToken = jwtProvider.resolveToken(httpServletRequest)

        if(accessToken.isNullOrBlank() || jwtProvider.isNotValidToken(accessToken)) {

            throw ApplicationException(AuthStatusCode.INVALID_JWT)
        }

        val username = jwtProvider.getUsernameFromToken(accessToken)
            ?: throw ApplicationException(AuthStatusCode.INVALID_JWT)

        val member = memberRepository.findByUsername(username)
            ?: throw ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)

        addCookie(
            "accessToken",
            null,
            0,
            true,
            httpServletResponse
        )

        addCookie(
            "refreshToken",
            null,
            0,
            true,
            httpServletResponse
        )

        redisTemplate.delete("accessToken:$username")
        refreshTokenRepository.deleteByMemberId(member.getId()!!)

        revokeAccessToken(accessToken)
    }

    override fun isRevokedAccessToken(
        accessToken: String
    ): Boolean {

        if (redisTemplate.hasKey(REVOKED_ACCESS_TOKEN_PREFIX + hash(accessToken))) {

            return true
        }

        return isIssuedBeforeRevocation(accessToken)
    }

    /**
     * 끊긴 시각 이전 토큰은 이 시각 이후 accessToken 수명이 지나면 모두 만료되므로 그만큼만 남긴다.
     * JWT의 발급 시각은 초 단위라 기준 시각도 초 단위로 저장한다.
     */
    override fun revokeAccessTokensIssuedBefore(
        username: String
    ) {

        redisTemplate.opsForValue().set(
            TOKEN_VALID_AFTER_PREFIX + username,
            Instant.now().epochSecond.toString(),
            accessTokenExpiration,
            TimeUnit.MILLISECONDS
        )
    }

    /**
     * 끊긴 시각과 같은 초에 새로 로그인한 토큰까지 막지 않도록, 그 초보다 앞선 토큰만 거부한다.
     */
    private fun isIssuedBeforeRevocation(
        accessToken: String
    ): Boolean {

        val username = jwtProvider.getUsernameFromToken(accessToken)
            ?: return false

        val validAfter = redisTemplate.opsForValue().get(TOKEN_VALID_AFTER_PREFIX + username)
            ?.toLongOrNull()
            ?: return false

        val issuedAt = jwtProvider.getIssuedAt(accessToken)
            ?: return true

        return issuedAt.toInstant().epochSecond < validAfter
    }

    override fun deleteTokensByMemberId(
        memberId: Long
    ) {

        /**
         * 쿠키를 지울 응답이 없는 경로에서 쓰므로 서버가 들고 있는 흔적만 지운다.
         * 이미 발급된 accessToken은 원문을 모르면 거부 목록에 넣을 수 없어 만료까지 유효하다.
         */
        refreshTokenRepository.deleteByMemberId(memberId)
    }

    /**
     * 토큰이 만료되면 어차피 거부되므로 거부 목록은 accessToken 수명만큼만 남긴다.
     * 토큰 원문 대신 해시를 키로 써서 Redis에 토큰이 그대로 남지 않게 한다.
     */
    private fun revokeAccessToken(
        accessToken: String
    ) {

        redisTemplate.opsForValue().set(
            REVOKED_ACCESS_TOKEN_PREFIX + hash(accessToken),
            "",
            accessTokenExpiration,
            TimeUnit.MILLISECONDS
        )
    }

    private fun hash(
        value: String
    ): String {

        return HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        )
    }

    override fun getCurrentUsername(httpServletRequest: HttpServletRequest) : String? {

        val accessToken = jwtProvider.resolveToken(httpServletRequest)
            ?.takeIf { it.isNotBlank() }
            ?: throw ApplicationException(AuthStatusCode.INVALID_JWT)

        /**
         * 공개 경로에서도 호출되므로 JWT 필터와 같은 기준으로 다시 확인한다.
         * 서명만 보면 refreshToken, 로그아웃한 토큰, 탈퇴 회원의 토큰도 통과한다.
         */
        return resolveActiveUsername(accessToken)
            ?: throw ApplicationException(AuthStatusCode.INVALID_JWT)
    }

    @Transactional(readOnly = true)
    override fun resolveActiveUsername(
        accessToken: String
    ): String? {

        if (
            jwtProvider.isNotValidToken(accessToken)
            || jwtProvider.isNotAccessToken(accessToken)
            || isRevokedAccessToken(accessToken)
        ) {

            return null
        }

        val username = jwtProvider.getUsernameFromToken(accessToken)
            ?.takeIf { it.isNotBlank() }
            ?: return null

        val member = memberRepository.findByUsername(username)
            ?: return null

        if (member.isDeleted()) {

            return null
        }

        return username
    }

    private fun extractTokenFromCookie(
        cookieName: String,
        httpServletRequest: HttpServletRequest
    ): String? {

        return httpServletRequest.cookies
            ?.firstOrNull { it.name == cookieName }
            ?.value
            ?.takeIf { it.isNotBlank() }
    }

    private fun addCookie(
        name: String,
        value: String?,
        age: Int,
        httpOnly: Boolean,
        httpServletResponse: HttpServletResponse?
    ) {

        /**
         * 도메인을 지정하지 않으면 쿠키가 발급 호스트(api.idta.store)에만 심겨
         * 프론트 도메인에서는 전송되지 않는다. 상위 도메인(.idta.store)을 지정해야
         * 서브도메인 간에 함께 실린다. 로컬처럼 값이 비어 있으면 기존대로 호스트 전용으로 둔다.
         *
         * 도메인 쿠키와 호스트 전용 쿠키는 브라우저에 별개로 남아 같은 이름이 2개씩 보이고,
         * 어느 쪽이 먼저 전송될지 보장되지 않는다. 그래서 도메인 쿠키를 심을 때는
         * 같은 이름의 호스트 전용 쿠키를 먼저 만료시켜 항상 하나만 남게 한다.
         */
        if (cookieDomain.isNotBlank()) {

            httpServletResponse?.addHeader(
                HttpHeaders.SET_COOKIE,
                buildCookie(
                    name,
                    "",
                    0,
                    httpOnly,
                    false
                ).toString()
            )
        }

        httpServletResponse?.addHeader(
            HttpHeaders.SET_COOKIE,
            buildCookie(
                name,
                value ?: "",
                age,
                httpOnly,
                true
            ).toString()
        )
    }

    private fun buildCookie(
        name: String,
        value: String,
        age: Int,
        httpOnly: Boolean,
        withDomain: Boolean
    ): ResponseCookie {

        return ResponseCookie.from(name, value)
            .path("/")
            .httpOnly(httpOnly)
            .secure(cookieSecure || cookieSameSite.equals("None", ignoreCase = true))
            .sameSite(cookieSameSite)
            .maxAge(age.toLong())
            .also { builder ->

                if (withDomain && cookieDomain.isNotBlank()) builder.domain(cookieDomain)
            }
            .build()
    }

    private fun toCookieMaxAge(expirationMillis: Long): Int {

        return (expirationMillis / 1000).toInt()
    }

    private fun toExpiresAt(expirationMillis: Long): LocalDateTime {

        return LocalDateTime.ofInstant(
            Instant.now().plusMillis(expirationMillis),
            ZoneId.systemDefault()
        )
    }
}
