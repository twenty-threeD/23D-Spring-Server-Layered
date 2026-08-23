package spring.springserver.global.jwt

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val tokenProvider: TokenProvider,
    private val memberDetailsService: MemberDetailsService
): OncePerRequestFilter() {

    override fun doFilterInternal(
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse,
        filterChain: FilterChain
    ) {

        val token = tokenProvider.resolveToken(httpServletRequest)

        if (token == null || tokenProvider.isNotValidToken(token) || tokenProvider.isNotAccessToken(token)) {

            filterChain.doFilter(
                httpServletRequest,
                httpServletResponse
            )

            return
        }

        val username = tokenProvider.getUsernameFromToken(token)

        if (username.isNullOrBlank()) {

            filterChain.doFilter(
                httpServletRequest,
                httpServletResponse
            )

            return
        }

        val memberDetails = runCatching {

            memberDetailsService.loadUserByUsername(username = username)
        }.getOrNull() as? MemberDetails

        if (memberDetails == null) {

            filterChain.doFilter(
                httpServletRequest,
                httpServletResponse
            )

            return
        }

        val authenticationToken = UsernamePasswordAuthenticationToken(
            memberDetails,
            null,
            memberDetails.authorities
        )

        SecurityContextHolder.getContext().authentication = authenticationToken

        filterChain.doFilter(
            httpServletRequest,
            httpServletResponse
        )
    }
}
