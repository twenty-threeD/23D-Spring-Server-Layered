package spring.springserver.domain.auth.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import spring.springserver.domain.auth.data.request.SignInRequest
import spring.springserver.domain.auth.data.request.SignUpRequest
import spring.springserver.domain.auth.data.response.SignInResponse
import spring.springserver.domain.auth.data.response.SignOutResponse
import spring.springserver.domain.auth.data.response.SignUpResponse
import spring.springserver.domain.auth.service.auth.AuthService
import spring.springserver.global.data.BaseResponse

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/signup")
    fun signUp(
        @Valid @RequestBody signUpRequest: SignUpRequest
    ): BaseResponse<SignUpResponse> {

        return BaseResponse.ok(authService.signUp(signUpRequest))
    }

    @PostMapping("/signin")
    fun signIn(
        @Valid @RequestBody signInRequest: SignInRequest,
        httpServletResponse: HttpServletResponse
    ): BaseResponse<SignInResponse> {

        return BaseResponse.ok(authService.signIn(
            signInRequest,
            httpServletResponse
            )
        )
    }

    @PostMapping("/signout")
    fun signOut(
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ) : BaseResponse<SignOutResponse> {

        return BaseResponse.ok(authService.signOut(
            httpServletRequest,
            httpServletResponse
            )
        )
    }
}