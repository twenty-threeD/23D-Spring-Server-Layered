package spring.springserver.domain.member.entity;

import com.l98293.phone.Phone;
import com.l98293.phone.Region;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
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

	@Email
	private String email;

	@Phone(region = Region.KR)
	private String phone;

	private String password;

	@Enumerated(EnumType.STRING)
	private Role role;

	public void setPassword(String password) {

		this.password = password;
	}
    // Member.java 엔티티 클래스 내부
    public Member update(String name) {

        this.name = name; // nane -> name으로 수정
        return this;
    }

    public String getRoleKey() {

        return this.role.name(); // 또는 role.getKey() 등 Role 이늄 구조에 맞게 설정
    }
}
