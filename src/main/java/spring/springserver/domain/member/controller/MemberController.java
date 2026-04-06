package spring.springserver.domain.member.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.springserver.domain.member.data.DeleteAccountResponse;
import spring.springserver.domain.member.service.MemberService;
import spring.springserver.global.data.BaseResponse;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @DeleteMapping("/api/auth/delete/account")
    public BaseResponse<DeleteAccountResponse> deleteAccount(
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {

        return memberService.deleteAccount(httpServletRequest, httpServletResponse);
    }
}
