package spring.springserver.domain.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.springserver.domain.auth.data.request.GenerateTokenRequest;
import spring.springserver.domain.auth.data.request.SignInRequest;
import spring.springserver.domain.auth.data.request.SignUpRequest;
import spring.springserver.domain.auth.data.response.SignInResponse;
import spring.springserver.domain.auth.data.response.SignUpResponse;
import spring.springserver.domain.auth.data.response.SignOutResponse;
import spring.springserver.domain.auth.exception.AuthStatusCode;
import spring.springserver.domain.member.entity.Member;
import spring.springserver.domain.member.repository.MemberRepository;
import spring.springserver.global.exception.exception.ApplicationException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;
    private final TokenService tokenService;

    /**
     * 회원가입
     * @param signUpRequest 회원가입 요청 값
     * @return message
     */
    public SignUpResponse signUp(SignUpRequest signUpRequest) {

        if (memberRepository.existsByUsername(signUpRequest.username())) {

            throw new ApplicationException(AuthStatusCode.USERNAME_ALREADY_EXIST);
        }

        if (memberRepository.existsByEmail(signUpRequest.email())) {

            throw new ApplicationException(AuthStatusCode.EMAIL_ALREADY_EXIST);
        }

        if (memberRepository.existsByPhone(signUpRequest.phone())) {

            throw new ApplicationException(AuthStatusCode.PHONE_ALREADY_EXIST);
        }

        memberRepository.save(signUpRequest.toEntity(passwordEncoder.encode(signUpRequest.password())));

        return SignUpResponse.of("회원가입이 완료되었습니다.");
    }

    /**
     * @param signInRequest 로그인 요청 값
     * @param httpServletResponse 쿠키 저장용
     * @return accessToken, refreshToken
     */
    @Transactional(readOnly = true)
    public SignInResponse signIn(SignInRequest signInRequest,
                                 HttpServletResponse httpServletResponse) {

        Member member = Optional.ofNullable(memberRepository.findByUsername(signInRequest.username()))
                .orElseThrow(
                        () -> new ApplicationException(AuthStatusCode.INVALID_CREDENTIALS)
                );

        if (!passwordEncoder.matches(signInRequest.password(), member.getPassword())) {

            throw new ApplicationException(AuthStatusCode.INVALID_CREDENTIALS);
        }

        GenerateTokenRequest generateTokenRequest = new GenerateTokenRequest (
            member.getUsername(),
            member.getRole()
        );

        return SignInResponse.of(
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

    /**
     *
     * @param httpServletRequest 쿠키용
     * @param httpServletResponse 쿠키용
     * @return message
     */
    public SignOutResponse signOut(HttpServletRequest httpServletRequest,
                                   HttpServletResponse httpServletResponse) {

        tokenService.deleteTokens(
                httpServletRequest,
                httpServletResponse
        );

        return SignOutResponse.of("로그아웃 되었습니다.");
    }
}