package spring.springserver.domain.auth.data.request

import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import spring.springserver.domain.member.entity.Role

data class GenerateTokenRequest(
    @field:NotBlank
    val username: String,

    @field:NotNull
    @field:Enumerated(EnumType.STRING)
    var role: Role
)