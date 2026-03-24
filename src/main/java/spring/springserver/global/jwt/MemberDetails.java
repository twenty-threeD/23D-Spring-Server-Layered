package spring.springserver.global.jwt;

import jakarta.validation.constraints.Email;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import spring.springserver.domain.member.entity.Member;

import java.util.Collection;
import java.util.List;

public record MemberDetails(Long id,
							String username,
							@Email String email,
							String password,
							Collection<? extends GrantedAuthority> authorities) implements UserDetails {

	public static MemberDetails from(Member member) {
		String role = member.getRole().name();
		if (!role.startsWith("ROLE_")) {
			role = "ROLE_" + role;
		}

		return new MemberDetails(
				member.getId(),
				member.getUsername(),
				member.getEmail(),
				member.getPassword(),
				List.of(new SimpleGrantedAuthority(role))
		);
	}

	@Override
	public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public @Nullable String getPassword() {
		return password();
	}

	@Override
	public @NonNull String getUsername() {
		return username();
	}
}
