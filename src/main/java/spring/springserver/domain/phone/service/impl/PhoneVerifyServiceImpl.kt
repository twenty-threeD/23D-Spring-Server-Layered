package spring.springserver.domain.phone.service.impl

import com.solapi.sdk.SolapiClient
import com.solapi.sdk.message.model.Message
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.auth.exception.AuthStatusCode
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.domain.payment.service.impl.PaymentServiceImpl
import spring.springserver.domain.phone.data.response.PhoneVerifyResponse
import spring.springserver.domain.phone.data.response.SendPhoneVerifyNumberResponse
import spring.springserver.domain.phone.exception.exception.CannotSendPhoneVerifyNumberException
import spring.springserver.domain.phone.exception.exception.CannotVerifyPhoneException
import spring.springserver.domain.phone.service.PhoneVerifyService
import spring.springserver.global.exception.exception.ApplicationException
import spring.springserver.global.util.PhoneNormalizer
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

@Service
@Transactional(rollbackFor = [Exception::class])
class PhoneVerifyServiceImpl(
    @param:Value($$"${solapi.api-key}")
    private val apiKey: String,

    @param:Value($$"${solapi.api-secret-key}")
    private val apiSecretKey: String,

    @param:Value($$"${solapi.sender-number}")
    private val senderNumber: String,

    private val redisTemplate: RedisTemplate<String, String>,

    private val memberRepository: MemberRepository
): PhoneVerifyService {

    private val secureRandom = SecureRandom()
    private val messageService = SolapiClient.createInstance(
        apiKey,
        apiSecretKey
    )

    override fun sendMessage(
        recipientNumber: String
    ): SendPhoneVerifyNumberResponse {

        val message = Message()
        val verifyCode = makeVerifyNumber()
        val recipientPhoneNumber = PhoneNormalizer.normalize(phone = recipientNumber)
            ?: throw CannotSendPhoneVerifyNumberException()

        message.from = PhoneNormalizer.normalize(phone = senderNumber)
        message.to = recipientPhoneNumber
        message.text = "잇다 본인인증 번호 안내\n [$verifyCode]"

        try {

            messageService.send(message = message)

            redisTemplate.opsForValue()
                .set(
                    "$recipientPhoneNumber:verify",
                    verifyCode,
                    3,
                    TimeUnit.MINUTES
                )
        } catch (e: Exception) {

            log.info("오류 :$e")

            throw CannotSendPhoneVerifyNumberException()
        }

        return SendPhoneVerifyNumberResponse("본인인증 번호가 발송되었습니다.")
    }

    override fun verifyPhone(
        recipientNumber: String,
        code: String
    ): PhoneVerifyResponse {

        val phoneNumber = PhoneNormalizer.normalize(phone = recipientNumber)
            ?: throw CannotVerifyPhoneException()
        val saved = redisTemplate.opsForValue().get("$phoneNumber:verify")
            ?: throw CannotVerifyPhoneException()

        if (saved != code) {

            throw CannotVerifyPhoneException()
        }

        redisTemplate.delete("$phoneNumber:verify")

        val member = getCurrentMemberOrNull()

        /**
         * 가입 시 인증 없이 아무 번호나 넣을 수 있으므로,
         * 인증 시점에 다른 회원이 이미 쓰는 번호인지 다시 확인한다.
         */
        if (member?.phone != phoneNumber && memberRepository.existsByPhone(phoneNumber)) {

            throw ApplicationException(AuthStatusCode.PHONE_ALREADY_EXIST)
        }

        /**
         * 로그인 상태면 회원에 바로 반영하고,
         * 가입 전이라면 회원가입이 소비할 수 있도록 인증 표식만 남긴다.
         */
        if (member != null) {

            member.verifyPhone(phone = phoneNumber)
        } else {

            redisTemplate.opsForValue()
                .set(
                    verifiedKey(phone = phoneNumber),
                    "true",
                    VERIFIED_MARK_MINUTES,
                    TimeUnit.MINUTES
                )
        }

        return PhoneVerifyResponse.of("본인인증이 완료되었습니다.")
    }

    override fun verifyCodeOnly(
        recipientNumber: String,
        code: String
    ): String {

        val phoneNumber = PhoneNormalizer.normalize(phone = recipientNumber)
            ?: throw CannotVerifyPhoneException()
        val saved = redisTemplate.opsForValue().get("$phoneNumber:verify")
            ?: throw CannotVerifyPhoneException()

        if (saved != code) {

            throw CannotVerifyPhoneException()
        }

        redisTemplate.delete("$phoneNumber:verify")

        return phoneNumber
    }

    override fun consumePhoneVerification(
        phone: String
    ): Boolean {

        val key = verifiedKey(phone = phone)

        if (redisTemplate.opsForValue().get(key) == null) {

            return false
        }

        redisTemplate.delete(key)

        return true
    }

    private fun getCurrentMemberOrNull() =
        SecurityContextHolder.getContext().authentication?.name
            ?.takeIf { username -> username.isNotBlank() && username != "anonymousUser" }
            ?.let { username -> memberRepository.findByUsername(username) }

    private fun verifiedKey(
        phone: String
    ): String = "$phone:verified"

    // 본인인증 번호 생성
    private fun makeVerifyNumber(): String =
        "%06d".format(secureRandom.nextInt(1_000_000))

    companion object {

        private const val VERIFIED_MARK_MINUTES = 30L

        private val log = LoggerFactory.getLogger(PaymentServiceImpl::class.java)
    }
}