package spring.springserver.domain.auth.data.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.member.entity.Provider
import spring.springserver.domain.member.entity.Role

data class SignUpRequest(
    @field:NotBlank
    val name: String,

    @field:NotBlank
    val username: String,

    @field:Email
    @field:NotBlank
    val email: String,

    @field:NotBlank
    val phone: String,

    @field:NotBlank
    @field:Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*(),.?\":{}|<>])[A-Za-z\\d!@#$%^&*(),.?\":{}|<>]{8,32}$",
        message = "비밀번호는 8자 이상 32자 이하이어야 하며, 영문 대소문자, 숫자, 특수문자를 각각 하나 이상 포함해야 합니다."
    )
    val password: String,

    @field:NotBlank
    val role: Role,

    @field:NotNull
    var provider: Provider
) {

        fun toEntity(encodedPassword: String): Member {

            return Member(
                username,
                name,
                email,
                phone,
                encodedPassword,
                Role.USER,
                provider
            )
        }
}