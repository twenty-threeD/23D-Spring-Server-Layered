package spring.springserver.domain.member.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Member {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String username;

	private String name;

	private String email;

	private String phone;

	private String password;

	@Enumerated(EnumType.STRING)
	private Role role;

	public void setPassword(String password) {
		this.password = password;
	}
}
