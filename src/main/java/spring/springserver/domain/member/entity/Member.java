package spring.springserver.domain.member.entity;

import com.l98293.phone.Phone;
import com.l98293.phone.Region;
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

	@Phone(region = Region.KR)
	private String phone;

	private String password;

	@Enumerated(EnumType.STRING)
	private Role role;

    public void setPassword(String password) {

        this.password = password;
    }

    // Member.java 내부에 아래 메서드가 정확히 이렇게 있어야 합니다.
    public Member update(String name) {

        this.name = name;

        return this; // 이 부분이 있어야 .map() 체이닝이 정상 작동합니다.
    }
  
    public void setUsername(String username) {

        this.username = username;
    }
}
