package spring.springserver.domain.auth.data.request;

import jakarta.validation.constraints.NotBlank;
import spring.springserver.domain.member.entity.Member;
import spring.springserver.domain.member.entity.Role;


public record SignupRequest(

        @NotBlank
        String name,

        @NotBlank
        String username,

        @NotBlank
        String email,

        @NotBlank
        String phone,

        @NotBlank
        String password,

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
