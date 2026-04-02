package spring.springserver.domain.auth.service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import spring.springserver.domain.auth.data.request.SigninRequest;
import spring.springserver.domain.auth.data.request.SignupRequest;
import spring.springserver.domain.auth.data.response.SignupResponse;
import spring.springserver.domain.auth.data.response.TokenResponse;
import spring.springserver.domain.auth.exception.AuthStatusCode;
import spring.springserver.domain.member.entity.Member;
import spring.springserver.domain.member.repository.MemberRepository;
import spring.springserver.global.data.BaseResponse;
import spring.springserver.global.exception.exception.ApplicationException;
import spring.springserver.global.jwt.JwtProvider;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    public BaseResponse<SignupResponse> signup(SignupRequest signupRequest) {

        if (memberRepository.existsByUsername(signupRequest.username())) {
            throw new ApplicationException(AuthStatusCode.USERNAME_ALREADY_EXIST);
        }

        memberRepository.save(signupRequest.toEntity(passwordEncoder.encode(signupRequest.password())));
        return BaseResponse.ok(SignupResponse.of("회원가입이 완료되었습니다."));
    }

    public BaseResponse<TokenResponse> signin(SigninRequest signinRequest,
                                              HttpServletResponse httpServletResponse) {

        Member member = memberRepository.findByUsername(signinRequest.username())
                .orElseThrow(
                        () -> new ApplicationException(AuthStatusCode.INVALID_CREDENTIALS)
                );

        if (!passwordEncoder.matches(signinRequest.password(), member.getPassword())) {
            throw new ApplicationException(AuthStatusCode.INVALID_CREDENTIALS);
        }

        return BaseResponse.ok(TokenResponse.of(
                jwtProvider.generateAccessToken(signinRequest.username(),member.getRole()),
                jwtProvider.generateRefreshToken(signinRequest.username())
                )
        );
    }
}

