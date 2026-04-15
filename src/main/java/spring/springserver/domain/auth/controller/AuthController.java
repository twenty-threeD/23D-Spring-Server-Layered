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
    public BaseResponse<MessageResponse> signUp(@RequestBody @Valid final SignUpRequest signUpRequest) {

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
    public BaseResponse<MessageResponse> signOut(HttpServletRequest httpServletRequest,
                                                 HttpServletResponse httpServletResponse) {

        return BaseResponse.ok(authService.signOut(
                httpServletRequest,
                httpServletResponse
                )
        );
    }

    @PostMapping("/password/reset")
    public BaseResponse<MessageResponse> resetPasswordWithoutAuth(@RequestBody @Valid final PasswordResetRequest request) {

        return BaseResponse.ok(authService.resetPasswordWithoutAuth(request));
    }

    @PostMapping("/password/reset/check")
    public BaseResponse<MessageResponse> resetPasswordWithAuth(HttpServletRequest httpServletRequest,
                                                                     HttpServletResponse httpServletResponse,
                                                                     @RequestBody @Valid final PasswordResetRequest request) {
        
        return BaseResponse.ok(
                authService.resetPasswordWithAuth(httpServletRequest, httpServletResponse, request)
        );
    }

    @GetMapping("/username/reset")
    public BaseResponse<FindUsernameResponse> findUsername(@RequestBody @Valid final EmailRequest usernameResetRequest) {

        return BaseResponse.ok(authService.findUsername(usernameResetRequest));
    }

    @PostMapping("/username/reset")
    public BaseResponse<MessageResponse> resetUsernameWithAuth(HttpServletRequest httpServletRequest,
                                                               HttpServletResponse httpServletResponse,
                                                               @RequestBody @Valid final ChangeUsernameRequest changeUsernameRequest) {

        return BaseResponse.ok(
                authService.resetUsernameWithAuth(httpServletRequest, httpServletResponse, changeUsernameRequest)
        );
    }
}