package spring.springserver.global.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import spring.springserver.domain.auth.interfaces.TokenProvider;

import java.io.IOException;
import java.security.AuthProvider;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

	private final TokenProvider tokenProvider;
    private final JwtProvider jwtProvider;

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request,
									@NonNull HttpServletResponse response,
									@NonNull FilterChain filterChain) throws ServletException, IOException {
		String token = tokenProvider.resolveToken(request);

		if (token == null || tokenProvider.isValidToken(token)) {
			filterChain.doFilter(request, response);
			return;
		}

		String username = tokenProvider.getUsernameFromToken(token);
		String role = tokenProvider.getRole(token).name();

		SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_ " + role);

		UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
				username,
				null,
				Collections.singletonList(authority));

		SecurityContextHolder.getContext().setAuthentication(authenticationToken);
		filterChain.doFilter(request, response);
	}

    private String resolveToken(HttpServletRequest httpServletRequest) {

        Cookie[] cookies = httpServletRequest.getCookies();
        if (cookies != null) {

            for (Cookie cookie : cookies) {

                if ("JWT_TOKEN".equals(cookie.getName())) {

                    return cookie.getValue();
                }
            }
        }

        String bearerToken = httpServletRequest.getHeader("Authorization");
        if (bearerToken != null &&  bearerToken.startsWith("Bearer ")) {

            return bearerToken.substring(7);
        }

        return null;
    }
}
