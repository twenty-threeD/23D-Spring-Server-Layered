package spring.springserver.domain.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.springserver.domain.auth.data.request.SignUpRequest;
import spring.springserver.domain.auth.data.request.SignInRequest;
import spring.springserver.domain.auth.data.request.PasswordResetRequest;
import spring.springserver.domain.auth.data.request.ChangeUsernameRequest;
import spring.springserver.domain.auth.data.request.FindUsernameRequest;
import spring.springserver.domain.auth.data.response.PasswordResetResponse;
import spring.springserver.domain.auth.data.response.SignInResponse;
import spring.springserver.domain.auth.data.response.SignOutResponse;
import spring.springserver.domain.auth.data.response.SignUpResponse;
import spring.springserver.domain.auth.data.response.FindUsernameResponse;
import spring.springserver.domain.auth.data.response.ChangeUsernameResponse;
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
                                                                     @RequestBody @Valid final PasswordResetRequest passwordResetRequest) {
        
        return BaseResponse.ok(
                authService.resetPasswordWithAuth(
                        httpServletRequest,
                        httpServletResponse,
                        passwordResetRequest
                )
        );
    }

    @GetMapping("/username")
    public BaseResponse<FindUsernameResponse> findUsername(@RequestBody @Valid final FindUsernameRequest findUsernameRequest) {

        return BaseResponse.ok(authService.findUsername(findUsernameRequest));
    }

    @PostMapping("/username/reset")
    public BaseResponse<ChangeUsernameResponse> resetUsernameWithAuth(HttpServletRequest httpServletRequest,
                                                                      HttpServletResponse httpServletResponse,
                                                                      @RequestBody @Valid final ChangeUsernameRequest changeUsernameRequest) {

        return BaseResponse.ok(
                authService.resetUsernameWithAuth(
                        httpServletRequest,
                        httpServletResponse,
                        changeUsernameRequest)
        );
    }
}