package spring.springserver.domain.auth.data.request;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.*;
import com.l98293.phone.Phone;
import com.l98293.phone.Region;
import spring.springserver.domain.member.entity.Member;
import spring.springserver.domain.member.entity.Role;


public record SignUpRequest(

        @NotBlank
        String name,

        @NotBlank
        String username,

        @Email
        @NotBlank
        String email,

        @NotBlank
        @Phone(region = Region.KR)
        String phone,

        @NotNull
        @Size(min = 8, max = 32, message = "비밀번호는 8자 이상 32자 이하여야 합니다.")
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*(),.?\":{}|<>])[A-Za-z\\d!@#$%^&*(),.?\":{}|<>]{8,}$",
                message = "비밀번호는 8자 이상이여야 하며, 영문 대소문자, 숫자, 특수문자를 각각 하나 이상 포함해야 합니다.")
        String password,

        @NotNull
        @Enumerated(EnumType.STRING)
        Role role
) {

    public Member toEntity(String encodedPassword) {

        return Member.builder()
                .username(username)
                .email(email)
                .phone(phone)
                .name(name)
                .password(encodedPassword)
                .role(role)
                .build();
    }
}