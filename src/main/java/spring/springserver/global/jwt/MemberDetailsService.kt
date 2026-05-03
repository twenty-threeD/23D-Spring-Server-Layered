package spring.springserver.global.jwt

import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.auth.exception.AuthStatusCode
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.global.exception.exception.ApplicationException

@Service
class MemberDetailsService(private val memberRepository: MemberRepository): UserDetailsService {

    @Transactional(readOnly = true)
    override fun loadUserByUsername(username: String): UserDetails? {

         return MemberDetails.from(memberRepository.findByUsername(username) ?: throw ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND))
    }
}