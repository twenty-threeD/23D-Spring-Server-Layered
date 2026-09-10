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

        val member = memberRepository.findByUsername(username)
            ?: throw ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)

        /**
         * 탈퇴한 회원은 행이 남아 있으므로 여기서 걸러야 기존 토큰으로도 인증되지 않는다.
         */
        if (member.isDeleted()) {

            throw ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)
        }

        return MemberDetails.from(member)
    }
}