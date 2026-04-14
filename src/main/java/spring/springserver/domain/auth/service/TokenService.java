package spring.springserver.domain.auth.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import spring.springserver.domain.auth.data.request.GenerateTokenRequest;
import spring.springserver.domain.auth.exception.AuthStatusCode;
import spring.springserver.global.exception.exception.ApplicationException;
import spring.springserver.global.jwt.JwtProvider;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class TokenService {

	private final JwtProvider jwtProvider;
	private final RedisTemplate<String, String> redisTemplate;

	public String generateAccessToken(GenerateTokenRequest generateTokenRequest,
									  HttpServletResponse httpServletResponse) {

		String accessToken = jwtProvider.generateAccessToken(generateTokenRequest);

		redisTemplate.opsForValue().set(
				"accessToken:" + generateTokenRequest.username(),
				accessToken,
				1,
				TimeUnit.HOURS
		);

		Cookie accessCookie = new Cookie("accessToken", accessToken);
		accessCookie.setPath("/");
		accessCookie.setHttpOnly(true);
		accessCookie.setMaxAge(60 * 60); // 1시간
		httpServletResponse.addCookie(accessCookie);

		return accessToken;
	}

	public String generateRefreshToken(GenerateTokenRequest generateTokenRequest,
									   HttpServletResponse httpServletResponse) {

		String refreshToken = jwtProvider.generateRefreshToken(generateTokenRequest);

		redisTemplate.opsForValue().set(
				"refreshToken:" + generateTokenRequest.username(),
				refreshToken,
				7,
				TimeUnit.DAYS
				);

		Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
		refreshCookie.setPath("/");
		refreshCookie.setHttpOnly(true);
		refreshCookie.setMaxAge(60 * 60 * 24 * 7); // 7일
		httpServletResponse.addCookie(refreshCookie);

		return refreshToken;
	}

	public void deleteTokens(HttpServletRequest httpServletRequest,
							 HttpServletResponse httpServletResponse) {

		String accessToken = extractTokenFromCookie(httpServletRequest, "accessToken");
		if (accessToken == null || accessToken.isBlank() || jwtProvider.isValidToken(accessToken)) {
			throw new ApplicationException(AuthStatusCode.INVALID_JWT);
		}

		String username = jwtProvider.getUsernameFromToken(accessToken);

		String savedRefreshToken = redisTemplate.opsForValue().get("refreshToken:" + username);
		String savedAccessToken = redisTemplate.opsForValue().get("accessToken:" + username);

		if (savedRefreshToken == null || savedAccessToken == null) {

			throw new ApplicationException(AuthStatusCode.ALREADY_LOGGED_OUT);
		} else {
			// 엑세스 토큰 만료(쿠키)
			Cookie accessCookie = new Cookie("accessToken", null);
			accessCookie.setPath("/");
			accessCookie.setHttpOnly(true);
			accessCookie.setMaxAge(0); // 즉시 만료
			httpServletResponse.addCookie(accessCookie);

			// 리프레시 토큰 만료(쿠키)
			Cookie refreshCookie = new Cookie("refreshToken", null);
			refreshCookie.setPath("/");
			refreshCookie.setHttpOnly(true);
			refreshCookie.setMaxAge(0); // 즉시 만료
			httpServletResponse.addCookie(refreshCookie);

			// Redis에서 삭제
			redisTemplate.delete("accessToken:" + username);
			redisTemplate.delete("refreshToken:" + username);
		}
	}

    public String extractTokenFromCookie(HttpServletRequest httpServletRequest,
                                         String cookieName) {

        Cookie[] cookies = httpServletRequest.getCookies();

        try {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {

                    return cookie.getValue();
                }
            }
        } catch (Exception e) {

			throw new ApplicationException(AuthStatusCode.INVALID_JWT);
		}

		return null;
	}
}
