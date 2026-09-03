package spring.springserver.domain.auth.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.*
import spring.springserver.domain.auth.data.request.ReissueTokenRequest
import spring.springserver.domain.auth.data.response.CurrentUsernameResponse
import spring.springserver.domain.auth.data.response.ReissueTokenResponse
import spring.springserver.domain.auth.exception.AuthStatusCode
import spring.springserver.domain.auth.service.token.TokenService
import spring.springserver.global.data.BaseResponse
import spring.springserver.global.exception.exception.ApplicationException

@RestController
@RequestMapping("/api/token")
class TokenController(
    private val tokenService: TokenService
) {

    @GetMapping("/username")
    fun getCurrentUsername(
        httpServletRequest: HttpServletRequest
    ): BaseResponse<CurrentUsernameResponse> {

        return BaseResponse.ok(
            CurrentUsernameResponse.of(
                tokenService.getCurrentUsername(
                    httpServletRequest = httpServletRequest
                ) ?: throw ApplicationException(AuthStatusCode.INVALID_JWT)
            )
        )
    }

    @PostMapping("/reissue")
    fun reissueAccessToken(
        @RequestBody(required = false) reissueTokenRequest: ReissueTokenRequest?,
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ): BaseResponse<ReissueTokenResponse> {

        return BaseResponse.ok(
            tokenService.reissueAccessToken(
                reissueTokenRequest,
                httpServletRequest,
                httpServletResponse
            )
        )
    }
}