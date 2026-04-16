package spring.springserver.domain.member.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import spring.springserver.domain.member.data.request.ChangeUsernameRequest;
import spring.springserver.domain.member.data.request.FindUsernameRequest;
import spring.springserver.domain.member.data.request.PasswordResetRequest;
import spring.springserver.domain.member.data.response.ChangeUsernameResponse;
import spring.springserver.domain.member.data.response.FindUsernameResponse;
import spring.springserver.domain.member.data.response.PasswordResetResponse;
import spring.springserver.domain.member.data.response.DeleteAccountResponse;
import spring.springserver.domain.member.service.MemberService;
import spring.springserver.global.data.BaseResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MemberController {

    private final MemberService memberService;

    @DeleteMapping("/delete/account")
    public BaseResponse<DeleteAccountResponse> deleteAccount(
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {

        return BaseResponse.ok(memberService.deleteAccount(
                        httpServletRequest,
                        httpServletResponse)
        );
    }

    @PostMapping("/password/reset")
    public BaseResponse<PasswordResetResponse> resetPasswordWithoutAuth(@RequestBody @Valid final PasswordResetRequest passwordResetRequest) {

        return BaseResponse.ok(memberService.resetPasswordWithoutAuth(passwordResetRequest));
    }

    @PostMapping("/password/reset/check")
    public BaseResponse<PasswordResetResponse> resetPasswordWithAuth(HttpServletRequest httpServletRequest,
                                                                     HttpServletResponse httpServletResponse,
                                                                     @RequestBody @Valid final PasswordResetRequest passwordResetRequest) {

        return BaseResponse.ok(
                memberService.resetPasswordWithAuth(
                        httpServletRequest,
                        httpServletResponse,
                        passwordResetRequest
                )
        );
    }

    @GetMapping("/username")
    public BaseResponse<FindUsernameResponse> findUsername(@RequestBody @Valid final FindUsernameRequest findUsernameRequest) {

        return BaseResponse.ok(memberService.findUsername(findUsernameRequest));
    }

    @PostMapping("/username/reset")
    public BaseResponse<ChangeUsernameResponse> resetUsernameWithAuth(HttpServletRequest httpServletRequest,
                                                                      HttpServletResponse httpServletResponse,
                                                                      @RequestBody @Valid final ChangeUsernameRequest changeUsernameRequest) {

        return BaseResponse.ok(
                memberService.resetUsernameWithAuth(
                        httpServletRequest,
                        httpServletResponse,
                        changeUsernameRequest)
        );
    }
}