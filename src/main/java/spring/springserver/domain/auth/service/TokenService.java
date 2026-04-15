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

		addCookie(
                "accessToken",
                accessToken,
                60 * 60,
                true,
                httpServletResponse);

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

		addCookie(
                "refreshToken",
                refreshToken,
                60 * 60 * 24 * 7,
                true,
                httpServletResponse);

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
			addCookie(
                    "accessToken",
                    null,
                    0,
                    false,
                    httpServletResponse);

			// 리프레시 토큰 만료(쿠키)
			addCookie(
                    "refreshToken",
                    null,
                    0,
                    true,
                    httpServletResponse);

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

    public String getCurrentUsername(HttpServletRequest httpServletRequest) {

        String accessToken = extractTokenFromCookie(httpServletRequest, "accessToken");

        if (accessToken == null || accessToken.isBlank()) {
            throw new ApplicationException(AuthStatusCode.INVALID_JWT);
        }

        return jwtProvider.getUsernameFromToken(accessToken);
    }

    private static void addCookie(String name,
                                  String value,
                                  int age,
                                  boolean httpOnly,
                                  HttpServletResponse httpServletResponse) {

        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(httpOnly);
        cookie.setMaxAge(age);

        httpServletResponse.addCookie(cookie);
    }
}
