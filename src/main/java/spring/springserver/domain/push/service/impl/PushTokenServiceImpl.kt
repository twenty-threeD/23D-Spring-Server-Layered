package spring.springserver.domain.push.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.domain.push.data.request.RegisterPushTokenRequest
import spring.springserver.domain.push.data.request.UnregisterPushTokenRequest
import spring.springserver.domain.push.entity.PushPlatform
import spring.springserver.domain.push.entity.PushToken
import spring.springserver.domain.push.exception.PushStatusCode
import spring.springserver.domain.push.repository.PushTokenRepository
import spring.springserver.domain.push.service.PushTokenService
import spring.springserver.global.exception.exception.ApplicationException

@Service
@Transactional(rollbackFor = [Exception::class])
class PushTokenServiceImpl(
    private val pushTokenRepository: PushTokenRepository,
    private val memberRepository: MemberRepository
): PushTokenService {

    override fun registerToken(
        registerPushTokenRequest: RegisterPushTokenRequest,
        memberId: Long?
    ) {

        val member = memberId?.let { id -> memberRepository.findById(id).orElse(null) }
            ?: throw ApplicationException(PushStatusCode.PUSH_MEMBER_NOT_FOUND)

        val platform = PushPlatform.valueOf(registerPushTokenRequest.platform.uppercase())

        val token = registerPushTokenRequest.token.trim()

        /**
         * 같은 기기가 다른 계정으로 로그인하면 토큰이 그대로 다시 온다.
         * 행을 새로 만들지 않고 회원만 바꿔 이전 계정으로는 더 이상 가지 않게 한다.
         */
        val existing = pushTokenRepository.findByToken(token)

        if (existing != null) {

            existing.reassign(
                member,
                platform
            )

            return
        }

        pushTokenRepository.save(
            PushToken(
                member = member,
                token = token,
                platform = platform
            )
        )
    }

    override fun unregisterToken(
        unregisterPushTokenRequest: UnregisterPushTokenRequest,
        memberId: Long?
    ) {

        val id = memberId
            ?: throw ApplicationException(PushStatusCode.PUSH_MEMBER_NOT_FOUND)

        /**
         * 남의 토큰을 지우지 못하게 본인 것만 지운다. 이미 없으면 아무것도 하지 않는다.
         */
        pushTokenRepository.deleteByTokenAndMemberId(
            unregisterPushTokenRequest.token.trim(),
            id
        )
    }
}
