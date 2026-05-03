package spring.springserver.global.jwt

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import spring.springserver.domain.auth.interfaces.TokenProvider
import java.util.Collections

@Component
class JwtAuthFilter(private val tokenProvider: TokenProvider): OncePerRequestFilter() {

    override fun doFilterInternal(
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse,
        filterChain: FilterChain
    ) {

        val token = tokenProvider.resolveToken(httpServletRequest)

        if (token == null || tokenProvider.isNotValidToken(token)) {

            filterChain.doFilter(
                httpServletRequest,
                httpServletResponse
            )

            return
        }

        val username = tokenProvider.getUsernameFromToken(token)
        val role = tokenProvider.getRole(token).name


        val authority = SimpleGrantedAuthority(role.takeIf { it.startsWith("ROLE_") } ?: "ROLE_$role")

        val authenticationToken = UsernamePasswordAuthenticationToken (
            username,
            null,
            Collections.singletonList(authority)
        )

        SecurityContextHolder.getContext().authentication = authenticationToken

        filterChain.doFilter(
            httpServletRequest,
            httpServletResponse
        )
    }
}