package spring.springserver.domain.auth.interfaces;

import jakarta.servlet.http.HttpServletRequest;
import spring.springserver.domain.auth.data.request.GenerateTokenRequest;
import spring.springserver.domain.member.entity.Role;

public interface TokenProvider {

	String generateRefreshToken(GenerateTokenRequest generateTokenRequest);
	String generateAccessToken(GenerateTokenRequest generateTokenRequest);

	String getUsernameFromToken(String token);

	Role getRole(String token);

	String resolveToken(HttpServletRequest httpServletRequest);

	boolean isValidToken(String token);
}