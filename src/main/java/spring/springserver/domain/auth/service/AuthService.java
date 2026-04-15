package spring.springserver.domain.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.springserver.domain.auth.data.request.*;
import spring.springserver.domain.auth.data.response.*;
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
    public MessageResponse signUp(SignUpRequest signUpRequest) {

        if (memberRepository.existsByUsername(signUpRequest.username())) {
            throw new ApplicationException(AuthStatusCode.USERNAME_ALREADY_EXIST);
        }

        memberRepository.save(signUpRequest.toEntity(passwordEncoder.encode(signUpRequest.password())));

        return MessageResponse.of("회원가입이 완료되었습니다.");
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
    public MessageResponse signOut(HttpServletRequest httpServletRequest,
                                                 HttpServletResponse httpServletResponse) {

        tokenService.deleteTokens(
                httpServletRequest,
                httpServletResponse
        );

        return MessageResponse.of("로그아웃 되었습니다.");
    }

    public MessageResponse resetPasswordWithoutAuth(PasswordResetRequest request) {

        Member member = memberRepository.findByUsername(request.username())
                .orElseThrow(
                        () -> new ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)
                );

        String encoded = passwordEncoder.encode(request.newPassword());
        member.setPassword(encoded);

        return MessageResponse.of("비밀번호가 변경되었습니다.");
    }

    public MessageResponse resetPasswordWithAuth(HttpServletRequest httpServletRequest,
                                                       HttpServletResponse httpServletResponse,
                                                       PasswordResetRequest request) {

        String accessToken = tokenService.extractTokenFromCookie(httpServletRequest, "accessToken");
        if (accessToken == null || accessToken.isBlank()) {
            throw new ApplicationException(AuthStatusCode.INVALID_JWT);
        }

        Member member = memberRepository.findByUsername(request.username())
                .orElseThrow(
                        () -> new ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)
                );

        String encoded = passwordEncoder.encode(request.newPassword());
        member.setPassword(encoded);

        tokenService.deleteTokens(
                httpServletRequest,
                httpServletResponse);

        return MessageResponse.of("비밀번호가 변경되었습니다. 다시 로그인 해주세요.");
    }

    public FindUsernameResponse findUsername(EmailRequest usernameResetRequest) {

        Member member = memberRepository.findByEmail(usernameResetRequest.email())
                .orElseThrow(
                        () -> new ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)
                );

        return FindUsernameResponse.of("아이디를 찾았습니다.", member.getUsername());
    }

    public MessageResponse resetUsernameWithAuth(HttpServletRequest httpServletRequest,
                                                 HttpServletResponse httpServletResponse,
                                                 ChangeUsernameRequest changeUsernameRequest) {

        String accessToken = tokenService.extractTokenFromCookie(httpServletRequest, "accessToken");
        if (accessToken == null || accessToken.isBlank()) {
            throw new ApplicationException(AuthStatusCode.INVALID_JWT);
        }

        String currentUsername = tokenService.getCurrentUsername(httpServletRequest);

        Member member = memberRepository.findByUsername(currentUsername)
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

        return MessageResponse.of("아이디가 변경 되었습니다. 다시 로그인 해주세요.");
    }
}
