package spring.springserver.domain.auth.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.springserver.domain.auth.data.request.SignInRequest;
import spring.springserver.domain.auth.data.request.SignUpRequest;
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

        return authService.signUp(signUpRequest);
    }

    @PostMapping("/signin")
    public BaseResponse<SignInResponse> signIn(@RequestBody @Valid final SignInRequest signInRequest,
                                               HttpServletResponse httpServletResponse) {

        return authService.signIn(signInRequest, httpServletResponse);
    }
}