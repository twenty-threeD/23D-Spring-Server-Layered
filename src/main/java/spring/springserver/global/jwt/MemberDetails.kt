package spring.springserver.global.jwt

import jakarta.validation.constraints.Email
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import spring.springserver.domain.member.entity.Member

data class MemberDetails(private val id: Long?,
                         private val username: String,
                         @field:Email private val email: String,
                         private val password: String,
                         private val authorities: Collection<GrantedAuthority>): UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority?> = authorities

    override fun getPassword(): String = password

    override fun getUsername(): String = username

    companion object {

        fun from(member: Member): MemberDetails {

            var role = member.role.name

            if (!role.startsWith("ROLE_")) {

                role = "ROLE_$role"
            }

            return MemberDetails(
                member.getId(),
                member.username,
                member.email,
                member.password,
                listOf(SimpleGrantedAuthority(role))
            )
        }
    }
}
