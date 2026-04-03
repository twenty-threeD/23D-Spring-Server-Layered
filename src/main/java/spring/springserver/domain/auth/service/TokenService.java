package spring.springserver.domain.auth.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import spring.springserver.domain.auth.data.request.GenerateTokenRequest;
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
}