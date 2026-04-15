package spring.springserver.domain.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.springserver.domain.auth.data.request.ChangeUsernameRequest;
import spring.springserver.domain.auth.data.request.GenerateTokenRequest;
import spring.springserver.domain.auth.data.request.SignInRequest;
import spring.springserver.domain.auth.data.request.SignUpRequest;
import spring.springserver.domain.auth.data.request.FindUsernameRequest;
import spring.springserver.domain.auth.data.request.PasswordResetRequest;
import spring.springserver.domain.auth.data.response.FindUsernameResponse;
import spring.springserver.domain.auth.data.response.PasswordResetResponse;
import spring.springserver.domain.auth.data.response.SignInResponse;
import spring.springserver.domain.auth.data.response.SignUpResponse;
import spring.springserver.domain.auth.data.response.SignOutResponse;
import spring.springserver.domain.auth.data.response.ChangeUsernameResponse;
import spring.springserver.domain.auth.exception.AuthStatusCode;
import spring.springserver.domain.member.entity.Member;
import spring.springserver.domain.member.repository.MemberRepository;
import spring.springserver.global.exception.exception.ApplicationException;

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

        memberRepository.save(signUpRequest.toEntity(passwordEncoder.encode(signUpRequest.password())));

        return SignUpResponse.of("회원가입이 완료되었습니다.");
    }

    /**
     * @param signInRequest 로그인 요청 값
     * @param httpServletResponse 쿠키 저장용
     * @return accessToken, refreshToken
     */
    @Transactional(readOnly = true)
    public SignInResponse signIn(
            SignInRequest signInRequest,
            HttpServletResponse httpServletResponse) {

        Member member = memberRepository.findByUsername(signInRequest.username())
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
                tokenService.generateAccessToken(generateTokenRequest, httpServletResponse),
                tokenService.generateRefreshToken(generateTokenRequest, httpServletResponse)
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

    public PasswordResetResponse resetPasswordWithoutAuth(PasswordResetRequest passwordResetRequest) {

        Member member = memberRepository.findByUsername(passwordResetRequest.username())
                .orElseThrow(
                        () -> new ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)
                );

        String encoded = passwordEncoder.encode(passwordResetRequest.newPassword());
        member.setPassword(encoded);

        return PasswordResetResponse.of("비밀번호가 변경되었습니다.");
    }

    public PasswordResetResponse resetPasswordWithAuth(HttpServletRequest httpServletRequest,
                                                       HttpServletResponse httpServletResponse,
                                                       PasswordResetRequest passwordResetRequest) {

        String accessToken = tokenService.extractTokenFromCookie(httpServletRequest, "accessToken");
        if (accessToken == null || accessToken.isBlank()) {
            throw new ApplicationException(AuthStatusCode.INVALID_JWT);
        }

        Member member = memberRepository.findByUsername(passwordResetRequest.username())
                .orElseThrow(
                        () -> new ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)
                );

        String encoded = passwordEncoder.encode(passwordResetRequest.newPassword());
        member.setPassword(encoded);

        tokenService.deleteTokens(
                httpServletRequest,
                httpServletResponse);

        return PasswordResetResponse.of("비밀번호가 변경되었습니다. 다시 로그인 해주세요.");
    }

    public FindUsernameResponse findUsername(FindUsernameRequest findUsernameRequest) {

        String username = memberRepository.findUsernameByEmail(findUsernameRequest.email())
                .orElseThrow(
                        () -> new ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)
                );

        return FindUsernameResponse.of("아이디를 찾았습니다.", username);
    }

    public ChangeUsernameResponse resetUsernameWithAuth(HttpServletRequest httpServletRequest,
                                                 HttpServletResponse httpServletResponse,
                                                 ChangeUsernameRequest changeUsernameRequest) {

        String accessToken = tokenService.extractTokenFromCookie(httpServletRequest, "accessToken");
        if (accessToken == null || accessToken.isBlank()) {
            throw new ApplicationException(AuthStatusCode.INVALID_JWT);
        }

        Member member = memberRepository.findByUsername(tokenService.getCurrentUsername(httpServletRequest))
                .orElseThrow(
                        () -> new ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)
                );

        if (!member.getEmail().equals(changeUsernameRequest.email())) {
            throw new ApplicationException(AuthStatusCode.INVALID_CREDENTIALS);
        }

        if (memberRepository.existsByUsername(changeUsernameRequest.newUsername())) {
            throw new ApplicationException(AuthStatusCode.USERNAME_ALREADY_EXIST);
        }

        member.setUsername(changeUsernameRequest.newUsername());

        tokenService.deleteTokens(
                httpServletRequest,
                httpServletResponse
        );

        return ChangeUsernameResponse.of("아이디가 변경 되었습니다. 다시 로그인 해주세요.");
    }
}
