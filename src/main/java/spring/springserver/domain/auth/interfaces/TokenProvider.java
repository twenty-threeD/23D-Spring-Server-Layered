package spring.springserver.domain.auth.interfaces;

import jakarta.servlet.http.HttpServletRequest;
import spring.springserver.domain.member.entity.Role;

public interface TokenProvider {

	String generateRefreshToken(String username);
	String generateAccessToken(String username, Role role);
	String getUsernameFromToken(String token);

	Role getRole(String token);

	String resolveToken(HttpServletRequest httpServletRequest);

	boolean isValidToken(String token);
}
