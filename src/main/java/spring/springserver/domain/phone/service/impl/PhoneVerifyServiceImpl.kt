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
import spring.springserver.domain.phone.data.response.PhoneVerifyResponse
import spring.springserver.domain.phone.data.response.SendPhoneVerifyNumberResponse
import spring.springserver.domain.phone.exception.exception.CannotSendPhoneVerifyNumberException
import spring.springserver.domain.phone.exception.exception.CannotVerifyPhoneException
import spring.springserver.domain.phone.exception.status_code.PhoneVerifyStatusCode
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

        /**
         * 발송은 로그인 없이 열려 있어 SMS 비용을 무제한으로 소모시킬 수 있다.
         * 번호마다 재발송 간격과 하루 발송 횟수를 제한한다.
         */
        limitSend(recipientPhoneNumber)

        message.from = PhoneNormalizer.normalize(phone = senderNumber)
        message.to = recipientPhoneNumber
        message.text = "잇다 본인인증 번호 안내\n [$verifyCode]"

        try {

            messageService.send(message = message)

            redisTemplate.opsForValue()
                .set(
                    verifyKey(phone = recipientPhoneNumber),
                    verifyCode,
                    3,
                    TimeUnit.MINUTES
                )

            redisTemplate.delete(attemptKey(phone = recipientPhoneNumber))
        } catch (e: Exception) {

            log.info("오류 :$e")

            redisTemplate.delete(sendCooldownKey(phone = recipientPhoneNumber))

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

        matchCode(
            phoneNumber,
            code
        )

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

        matchCode(
            phoneNumber,
            code
        )

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

    /**
     * 인증번호가 6자리라 시도 횟수를 막지 않으면 3분 안에 전부 대입해 볼 수 있다.
     * 비교보다 먼저 횟수를 올려야 동시에 몰린 요청도 한도 이상 비교되지 않는다.
     * 한도에 닿으면 인증번호를 폐기해 다시 발송받게 한다.
     */
    private fun matchCode(
        phoneNumber: String,
        code: String
    ) {

        val saved = redisTemplate.opsForValue().get(verifyKey(phone = phoneNumber))
            ?: throw CannotVerifyPhoneException()

        val attempts = redisTemplate.opsForValue().increment(attemptKey(phone = phoneNumber))
            ?: throw CannotVerifyPhoneException()

        if (attempts == 1L) {

            redisTemplate.expire(
                attemptKey(phone = phoneNumber),
                3,
                TimeUnit.MINUTES
            )
        }

        if (attempts > MAX_VERIFY_ATTEMPTS) {

            redisTemplate.delete(
                listOf(
                    verifyKey(phone = phoneNumber),
                    attemptKey(phone = phoneNumber)
                )
            )

            throw ApplicationException(PhoneVerifyStatusCode.VERIFY_ATTEMPTS_EXCEEDED)
        }

        if (saved != code) {

            throw CannotVerifyPhoneException()
        }

        redisTemplate.delete(
            listOf(
                verifyKey(phone = phoneNumber),
                attemptKey(phone = phoneNumber)
            )
        )
    }

    private fun limitSend(
        phoneNumber: String
    ) {

        val acquired = redisTemplate.opsForValue().setIfAbsent(
            sendCooldownKey(phone = phoneNumber),
            "1",
            SEND_COOLDOWN_SECONDS,
            TimeUnit.SECONDS
        ) ?: false

        if (!acquired) {

            throw ApplicationException(PhoneVerifyStatusCode.SEND_TOO_FREQUENT)
        }

        val sendCount = redisTemplate.opsForValue().increment(sendDailyKey(phone = phoneNumber))
            ?: throw CannotSendPhoneVerifyNumberException()

        if (sendCount == 1L) {

            redisTemplate.expire(
                sendDailyKey(phone = phoneNumber),
                1,
                TimeUnit.DAYS
            )
        }

        if (sendCount > MAX_DAILY_SEND_COUNT) {

            throw ApplicationException(PhoneVerifyStatusCode.SEND_DAILY_LIMIT_EXCEEDED)
        }
    }

    private fun verifyKey(
        phone: String
    ): String = "$phone:verify"

    private fun attemptKey(
        phone: String
    ): String = "$phone:verify:attempts"

    private fun sendCooldownKey(
        phone: String
    ): String = "$phone:send:cooldown"

    private fun sendDailyKey(
        phone: String
    ): String = "$phone:send:daily"

    private fun verifiedKey(
        phone: String
    ): String = "$phone:verified"

    // 본인인증 번호 생성
    private fun makeVerifyNumber(): String =
        "%06d".format(secureRandom.nextInt(1_000_000))

    companion object {

        private const val VERIFIED_MARK_MINUTES = 30L

        private const val MAX_VERIFY_ATTEMPTS = 5L

        private const val SEND_COOLDOWN_SECONDS = 60L

        private const val MAX_DAILY_SEND_COUNT = 10L

        private val log = LoggerFactory.getLogger(PhoneVerifyServiceImpl::class.java)
    }
}