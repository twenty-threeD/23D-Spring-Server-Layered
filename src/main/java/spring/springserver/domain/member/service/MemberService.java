package spring.springserver.domain.member.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.springserver.domain.member.data.request.ChangeUsernameRequest;
import spring.springserver.domain.member.data.request.FindUsernameRequest;
import spring.springserver.domain.member.data.request.PasswordResetRequest;
import spring.springserver.domain.member.data.response.ChangeUsernameResponse;
import spring.springserver.domain.member.data.response.FindUsernameResponse;
import spring.springserver.domain.member.data.response.PasswordResetResponse;
import spring.springserver.domain.auth.exception.AuthStatusCode;
import spring.springserver.domain.auth.service.TokenService;
import spring.springserver.domain.member.data.response.DeleteAccountResponse;
import spring.springserver.domain.member.entity.Member;
import spring.springserver.domain.member.repository.MemberRepository;
import spring.springserver.global.exception.exception.ApplicationException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class MemberService {

    private final MemberRepository memberRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    public DeleteAccountResponse deleteAccount(HttpServletRequest httpServletRequest,
                                               HttpServletResponse httpServletResponse) {

        String username = tokenService.getCurrentUsername(httpServletRequest);

        Member member = Optional.ofNullable(memberRepository.findByUsername(username))
                .orElseThrow(
                        () -> new ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)
                );

        tokenService.deleteTokens(
                httpServletRequest,
                httpServletResponse
        );

        memberRepository.delete(member);

        return DeleteAccountResponse.of("탈퇴되었습니다.");
    }

    public PasswordResetResponse resetPasswordWithoutAuth(PasswordResetRequest passwordResetRequest) {

        Member member = Optional.ofNullable(memberRepository.findByUsername(passwordResetRequest.username()))
                .orElseThrow(
                        () -> new ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)
                );

        member.setPassword(passwordEncoder.encode(passwordResetRequest.newPassword()));

        return PasswordResetResponse.of("비밀번호가 변경되었습니다.");
    }

    public PasswordResetResponse resetPasswordWithAuth(PasswordResetRequest passwordResetRequest,
                                                       HttpServletRequest httpServletRequest,
                                                       HttpServletResponse httpServletResponse) {

        String accessToken = tokenService.extractTokenFromCookie(
                "accessToken",
                httpServletRequest
        );

        if (accessToken == null || accessToken.isBlank()) {

            throw new ApplicationException(AuthStatusCode.INVALID_JWT);
        }

        Member member = Optional.ofNullable(memberRepository.findByUsername(passwordResetRequest.username()))
                .orElseThrow(
                        () -> new ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)
                );

        String encoded = passwordEncoder.encode(passwordResetRequest.newPassword());
        member.setPassword(encoded);

        tokenService.deleteTokens(
                httpServletRequest,
                httpServletResponse
        );

        return PasswordResetResponse.of("비밀번호가 변경되었습니다. 다시 로그인 해주세요.");
    }

    public FindUsernameResponse findUsername(FindUsernameRequest findUsernameRequest) {

        String username = Optional.ofNullable(memberRepository.findUsernameByEmail(findUsernameRequest.email()))
                .orElseThrow(
                        () -> new ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)
                );

        return FindUsernameResponse.of(username);
    }

    public ChangeUsernameResponse resetUsernameWithAuth(ChangeUsernameRequest changeUsernameRequest,
                                                        HttpServletRequest httpServletRequest,
                                                        HttpServletResponse httpServletResponse) {

        String accessToken = tokenService.extractTokenFromCookie(
                "accessToken",
                httpServletRequest
        );

        if (accessToken == null || accessToken.isBlank()) {

            throw new ApplicationException(AuthStatusCode.INVALID_JWT);
        }

        Member member = Optional.ofNullable(memberRepository.findByUsername(tokenService.getCurrentUsername(httpServletRequest)))
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