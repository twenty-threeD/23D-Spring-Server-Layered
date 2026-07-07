package spring.springserver.domain.oauth.data.request

import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import spring.springserver.domain.member.entity.Role

/**
 * @author @gnlandkmg(개발)
 * @author @L98293(코틀린 변환)
 */
data class OAuthRequest(

    @field:NotBlank
    val username: String,

    @field:Email
    @field:NotBlank
    val email: String,

    @field:NotBlank
    val name: String,

    @field:Enumerated(EnumType.STRING)
    val role: Role
)