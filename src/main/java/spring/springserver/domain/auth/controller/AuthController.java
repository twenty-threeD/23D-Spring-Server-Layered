package spring.springserver.domain.auth.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.springserver.domain.auth.data.request.SigninRequest;
import spring.springserver.domain.auth.data.request.SignupRequest;
import spring.springserver.domain.auth.data.response.SignupResponse;
import spring.springserver.domain.auth.data.response.TokenResponse;
import spring.springserver.domain.auth.service.AuthService;
import spring.springserver.global.data.BaseResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public BaseResponse<SignupResponse> signup(@RequestBody @Valid SignupRequest signUpRequest) {

        return authService.signup(signUpRequest);
    }

    @PostMapping("/signin")
    public BaseResponse<TokenResponse> signin(@RequestBody @Valid SigninRequest signInRequest, HttpServletResponse httpServletResponse) {

        return authService.signin(signInRequest, httpServletResponse);
    }
}
