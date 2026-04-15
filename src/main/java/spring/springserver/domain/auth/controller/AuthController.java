package spring.springserver.domain.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import spring.springserver.domain.auth.data.request.*;
import spring.springserver.domain.auth.data.response.*;
import spring.springserver.domain.auth.service.AuthService;
import spring.springserver.global.data.BaseResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public BaseResponse<SignUpResponse> signUp(@RequestBody @Valid final SignUpRequest signUpRequest) {

        return BaseResponse.ok(authService.signUp(signUpRequest));
    }

    @PostMapping("/signin")
    public BaseResponse<SignInResponse> signIn(@RequestBody @Valid final SignInRequest signInRequest,
                                               HttpServletResponse httpServletResponse) {

        return BaseResponse.ok(authService.signIn(
                signInRequest,
                httpServletResponse
                )
        );
    }

    @PostMapping("/signout")
    public BaseResponse<SignOutResponse> signOut(HttpServletRequest httpServletRequest,
                                                 HttpServletResponse httpServletResponse) {

        return BaseResponse.ok(authService.signOut(
                httpServletRequest,
                httpServletResponse
                )
        );
    }

    @PostMapping("/password/reset")
    public BaseResponse<PasswordResetResponse> resetPasswordWithoutAuth(@RequestBody @Valid final PasswordResetRequest passwordResetRequest) {

        return BaseResponse.ok(authService.resetPasswordWithoutAuth(passwordResetRequest));
    }

    @PostMapping("/password/reset/check")
    public BaseResponse<PasswordResetResponse> resetPasswordWithAuth(HttpServletRequest httpServletRequest,
                                                                     HttpServletResponse httpServletResponse,
                                                                     @RequestBody @Valid final PasswordResetRequest request) {
        
        return BaseResponse.ok(
                authService.resetPasswordWithAuth(httpServletRequest, httpServletResponse, request)
        );
    }

    @GetMapping("/username/reset")
    public BaseResponse<FindUsernameResponse> findUsername(@RequestBody @Valid final FindUsernameRequest findUsernameRequest) {

        return BaseResponse.ok(authService.findUsername(findUsernameRequest));
    }

    @PostMapping("/username/reset")
    public BaseResponse<ChangeUsernameResponse> resetUsernameWithAuth(HttpServletRequest httpServletRequest,
                                                               HttpServletResponse httpServletResponse,
                                                               @RequestBody @Valid final ChangeUsernameRequest changeUsernameRequest) {

        return BaseResponse.ok(
                authService.resetUsernameWithAuth(httpServletRequest, httpServletResponse, changeUsernameRequest)
        );
    }
}