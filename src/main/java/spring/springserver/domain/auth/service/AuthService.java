package spring.springserver.domain.auth.service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import spring.springserver.domain.auth.data.request.GenerateTokenRequest;
import spring.springserver.domain.auth.data.request.SignInRequest;
import spring.springserver.domain.auth.data.request.SignUpRequest;
import spring.springserver.domain.auth.data.response.SignUpResponse;
import spring.springserver.domain.auth.data.response.SignInResponse;
import spring.springserver.domain.auth.exception.AuthStatusCode;
import spring.springserver.domain.member.entity.Member;
import spring.springserver.domain.member.repository.MemberRepository;
import spring.springserver.global.data.BaseResponse;
import spring.springserver.global.exception.exception.ApplicationException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;
    private final TokenService tokenService;

    public BaseResponse<SignUpResponse> signUp(SignUpRequest signupRequest) {

        if (memberRepository.existsByUsername(signupRequest.username())) {
            throw new ApplicationException(AuthStatusCode.USERNAME_ALREADY_EXIST);
        }

        memberRepository.save(signupRequest.toEntity(passwordEncoder.encode(signupRequest.password())));
        return BaseResponse.ok(SignUpResponse.of("회원가입이 완료되었습니다."));
    }

    public BaseResponse<SignInResponse> signIn(SignInRequest signinRequest,
                                               HttpServletResponse httpServletResponse) {

        Member member = memberRepository.findByUsername(signinRequest.username())
                .orElseThrow(
                        () -> new ApplicationException(AuthStatusCode.INVALID_CREDENTIALS)
                );

        if (!passwordEncoder.matches(signinRequest.password(), member.getPassword())) {
            throw new ApplicationException(AuthStatusCode.INVALID_CREDENTIALS);
        }

        GenerateTokenRequest generateTokenRequest = new GenerateTokenRequest (
            member.getUsername(),
            member.getRole()
        );

        return BaseResponse.ok(SignInResponse.of(
                tokenService.generateRefreshToken(generateTokenRequest, httpServletResponse),
                tokenService.generateAccessToken(generateTokenRequest, httpServletResponse)
                )
        );
    }
}