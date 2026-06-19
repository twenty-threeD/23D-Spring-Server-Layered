package spring.springserver.domain.member.service.impl

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.auth.exception.AuthStatusCode
import spring.springserver.domain.auth.service.token.TokenService
import spring.springserver.domain.member.data.request.FindUsernameRequest
import spring.springserver.domain.member.data.request.PasswordResetRequest
import spring.springserver.domain.member.data.response.DeleteAccountResponse
import spring.springserver.domain.member.data.response.FindUsernameResponse
import spring.springserver.domain.member.data.response.PasswordResetResponse
import spring.springserver.domain.member.data.response.UsernameCheckResponse
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.domain.member.service.MemberService
import spring.springserver.global.exception.exception.ApplicationException

@Service
@Transactional(rollbackFor = [Exception::class])
class MemberServiceImpl(
    private val memberRepository: MemberRepository,
    private val tokenService: TokenService,
    private val passwordEncoder: PasswordEncoder
) : MemberService {

    override fun deleteAccount(
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ): DeleteAccountResponse {

        val username = tokenService.getCurrentUsername(httpServletRequest)

        val member = memberRepository.findByUsername(username)
            ?: throw ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)

        tokenService.deleteTokens(
            httpServletRequest,
            httpServletResponse,
        )

        memberRepository.delete(member)

        return DeleteAccountResponse.of("탈퇴되었습니다.")
    }

    override fun resetPasswordWithoutAuth(
        passwordResetRequest: PasswordResetRequest
    ): PasswordResetResponse {

        val member = memberRepository.findByUsername(passwordResetRequest.username)
            ?: throw ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)

        member.password = passwordEncoder.encode(passwordResetRequest.newPassword)

        return PasswordResetResponse.of("비밀번호가 변경되었습니다.")
    }

    @Transactional(readOnly = true)
    override fun resetPasswordWithAuth(
        passwordResetRequest: PasswordResetRequest,
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ): PasswordResetResponse {

        val accessToken = tokenService.extractTokenFromCookie(
            "accessToken",
            httpServletRequest,
        )

        if(accessToken.isNullOrBlank()) {

            throw ApplicationException(AuthStatusCode.INVALID_JWT)
        }

        val member = memberRepository.findByUsername(passwordResetRequest.username)
            ?: throw ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)

        val encoded = passwordEncoder.encode(passwordResetRequest.newPassword)
        member.password = encoded

        tokenService.deleteTokens(
            httpServletRequest,
            httpServletResponse,
        )

        return PasswordResetResponse.of("비밀번호가 변경되었습니다. 다시 로그인 해주세요.")
    }

    override fun findUsername(
        findUsernameRequest: FindUsernameRequest
    ): FindUsernameResponse {

        val username = memberRepository.findUsernameByEmail(findUsernameRequest.email)
            ?: throw ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)

        return FindUsernameResponse.of(username)
    }

    @Transactional(readOnly = true)
    override fun checkUsername(
        username: String
    ): UsernameCheckResponse {

        return UsernameCheckResponse.of(!memberRepository.existsByUsername(username))
    }
}