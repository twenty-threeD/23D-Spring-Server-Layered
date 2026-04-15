package spring.springserver.domain.member.entity;

import com.l98293.phone.Phone;
import com.l98293.phone.Region;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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

    @Setter
	private String username;

	private String name;

	private String email;

	@Phone(region = Region.KR)
	private String phone;

    @Setter
	private String password;

	@Enumerated(EnumType.STRING)
	private Role role;
}
