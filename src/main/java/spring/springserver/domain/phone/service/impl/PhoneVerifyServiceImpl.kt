package spring.springserver.domain.phone.service.impl

import com.solapi.sdk.message.model.Message
import com.solapi.sdk.SolapiClient

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.payment.service.impl.PaymentServiceImpl
import spring.springserver.domain.phone.data.response.PhoneVerifyResponse
import spring.springserver.domain.phone.data.response.SendPhoneVerifyNumberResponse
import spring.springserver.domain.phone.exception.exception.CannotSendPhoneVerifyNumberException
import spring.springserver.domain.phone.exception.exception.CannotVerifyPhoneException
import spring.springserver.domain.phone.service.PhoneVerifyService
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

    private val redisTemplate: RedisTemplate<String, String>
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

        return PhoneVerifyResponse.of("본인인증이 완료되었습니다.")
    }

    // 본인인증 번호 생성
    private fun makeVerifyNumber(): String =
        "%06d".format(secureRandom.nextInt(1_000_000))

    companion object {

        private val log = LoggerFactory.getLogger(PaymentServiceImpl::class.java)
    }
}