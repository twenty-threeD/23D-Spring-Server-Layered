package spring.springserver.domain.auth.service.auth

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import spring.springserver.domain.auth.data.request.SignInRequest
import spring.springserver.domain.auth.data.request.SignUpRequest
import spring.springserver.domain.auth.data.response.SignInResponse
import spring.springserver.domain.auth.data.response.SignOutResponse
import spring.springserver.domain.auth.data.response.SignUpResponse

interface AuthService {

    fun signUp(
        signUpRequest: SignUpRequest
    ) : SignUpResponse

    fun signIn(
        signInRequest: SignInRequest,
        httpServletResponse: HttpServletResponse
    ) : SignInResponse

    fun signOut(
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ) : SignOutResponse
}