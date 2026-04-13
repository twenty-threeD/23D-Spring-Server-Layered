package spring.springserver.domain.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import spring.springserver.domain.auth.data.request.PasswordResetRequest;
import spring.springserver.domain.auth.data.request.SignInRequest;
import spring.springserver.domain.auth.data.request.SignUpRequest;
import spring.springserver.domain.auth.data.response.PasswordResetResponse;
import spring.springserver.domain.auth.data.response.SignOutResponse;
import spring.springserver.domain.auth.data.response.SignUpResponse;
import spring.springserver.domain.auth.data.response.SignInResponse;
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
    public BaseResponse<PasswordResetResponse> resetPasswordWithoutAuth(@RequestBody @Valid final PasswordResetRequest request) {
        return BaseResponse.ok(authService.resetPasswordWithoutAuth(request));
    }

    @PostMapping("/password/reset/check")
    public BaseResponse<PasswordResetResponse> resetPasswordWithAuth(HttpServletRequest httpServletRequest,
                                                                     HttpServletResponse httpServletResponse,
                                                                     @RequestBody @Valid final PasswordResetRequest request) {
        return BaseResponse.ok(
                authService.resetPasswordWithAuth(httpServletRequest, httpServletResponse, request)
        );
    }
}