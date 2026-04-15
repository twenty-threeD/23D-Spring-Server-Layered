package spring.springserver.domain.member.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.springserver.domain.auth.exception.AuthStatusCode;
import spring.springserver.domain.auth.service.TokenService;
import spring.springserver.domain.member.data.response.DeleteAccountResponse;
import spring.springserver.domain.member.entity.Member;
import spring.springserver.domain.member.repository.MemberRepository;
import spring.springserver.global.exception.exception.ApplicationException;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class MemberService {

    private final MemberRepository memberRepository;
    private final TokenService tokenService;

    public DeleteAccountResponse deleteAccount(HttpServletRequest httpServletRequest,
                                               HttpServletResponse httpServletResponse) {

        String username = tokenService.getCurrentUsername(httpServletRequest);

        Member member = memberRepository.findByUsername(username)
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
}