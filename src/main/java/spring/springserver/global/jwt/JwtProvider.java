package spring.springserver.global.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import spring.springserver.domain.auth.data.request.GenerateTokenRequest;
import spring.springserver.domain.auth.interfaces.TokenProvider;
import spring.springserver.domain.member.entity.Role;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtProvider implements TokenProvider {

	private final SecretKey secretKey;
	private final Long refreshTokenExpiration;
	private final Long accessTokenExpiration;

	public JwtProvider(@Value("${spring.jwt.secret}") String secret,
					   @Value("${spring.jwt.refreshTokenExpiration}") Long refreshTokenExpiration,
					   @Value("${spring.jwt.accessTokenExpiration}") Long accessTokenExpiration) {

		this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.refreshTokenExpiration = refreshTokenExpiration;
		this.accessTokenExpiration = accessTokenExpiration;
	}

	@Override
	public String generateRefreshToken(GenerateTokenRequest generateTokenRequest) {

		Date now = new Date();
		Date expiration = new Date(now.getTime() + refreshTokenExpiration);

		return Jwts.builder()
				.subject(generateTokenRequest.username())

				.claim("tokenType", "refreshToken")

				.issuedAt(now)
				.expiration(expiration)
				.signWith(secretKey)

				.compact();
	}

	@Override
	public String generateAccessToken(GenerateTokenRequest generateTokenRequest) {

		Date now = new Date();
		Date expiration = new Date(now.getTime() + accessTokenExpiration);

		return Jwts.builder()
				.subject(generateTokenRequest.username())

				.claim("role", generateTokenRequest.role())
				.claim("tokenType", "accessToken")

				.issuedAt(now)
				.expiration(expiration)
				.signWith(secretKey)

				.compact();
	}

	@Override
	public String getUsernameFromToken(String token) {

		return getClaims(token).getSubject();
	}

	@Override
	public Role getRole(String token) {

		String role = getClaims(token).get("role", String.class);

		return role == null ? null : Role.valueOf(role);
	}

	// 해당 메서드는 유효한 경우 항상 false를, 유효하지 않은 경우 true를 반환합니다
	@Override
	public boolean isValidToken(String token) {

		try {
			getClaims(token);

			return false;
		} catch (JwtException | IllegalArgumentException e) {

			return true;
		}
	}

	@Override
	public String resolveToken(HttpServletRequest request) {

		String token = request.getHeader("Authorization");
		if (token != null && token.startsWith("Bearer ")) {

			return token.substring(7);
		}

		return null;
	}

	private Claims getClaims(String token) {

		return Jwts.parser()
					.verifyWith(secretKey)
					.build()
					.parseSignedClaims(token)
					.getPayload();
	}
}
