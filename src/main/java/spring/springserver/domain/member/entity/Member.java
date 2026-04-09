package spring.springserver.domain.member.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
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

    private String provider;

    @Builder
    public Member(String username,
                  String name,
                  String email,
                  String phone,
                  String password,
                  Role role,
                  String provider) {
        this.username = username;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.role = role;
        this.provider = provider;
    }

    public Member update(String name) {
        this.name = name;
        return this;
    }

	public void setPassword(String password) {
		this.password = password;
	}
}
