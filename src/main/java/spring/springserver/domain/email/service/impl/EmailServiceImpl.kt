package spring.springserver.domain.email.service.impl

import jakarta.mail.MessagingException
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import spring.springserver.domain.email.data.response.CheckVerifyCodeResponse
import spring.springserver.domain.email.data.response.SendVerifyCodeResponse
import spring.springserver.domain.email.exception.EmailException
import spring.springserver.domain.email.service.EmailService
import spring.springserver.global.config.redis.RedisConfig
import spring.springserver.global.exception.exception.ApplicationException
import java.io.UnsupportedEncodingException
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

class EmailServiceImpl(private val javaMailSender: JavaMailSender,
                       private val redisConfig: RedisConfig,
                       private val templateEngine: TemplateEngine): EmailService {

    @Value($$"${spring.mail.username}")
    private lateinit var serviceName: String

    fun makeRandomCode(): String {

        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ1234567890"
        val secureRandom = SecureRandom()

        return (1..8)
            .map { chars[secureRandom.nextInt(chars.length)] }
            .joinToString("")
    }

    override fun sendVerifyCode(email: String): SendVerifyCodeResponse {

        return sendAndCache(
            email,
            signupKey(email)
        )
    }

    /**
     * 인증에 성공하면 가입이 소비할 수 있도록 인증 표식을 남긴다.
     * 가입 화면은 코드 확인과 가입 요청이 따로 오므로, 코드만 맞히고 가입은 다른 이메일로 하는 것을 막으려면
     * 가입 시점에 이 표식을 확인해야 한다.
     */
    override fun checkVerifyCode(email: String,
                                 code: String): CheckVerifyCodeResponse {

        val response = checkAndConsume(
            code,
            signupKey(email)
        )

        redisConfig.redisTemplate()
            .opsForValue()
            .set(
                verifiedKey(email),
                "true",
                VERIFIED_MARK_MINUTES,
                TimeUnit.MINUTES
            )

        return response
    }

    override fun consumeEmailVerification(email: String): Boolean {

        return redisConfig.redisTemplate().delete(verifiedKey(email))
    }

    override fun sendChangeEmailCode(email: String): SendVerifyCodeResponse {

        return sendAndCache(
            email,
            changeEmailKey(email)
        )
    }

    override fun checkChangeEmailCode(email: String,
                                      code: String): CheckVerifyCodeResponse {

        return checkAndConsume(
            code,
            changeEmailKey(email)
        )
    }

    private fun sendAndCache(email: String,
                             redisKey: String): SendVerifyCodeResponse {

        /**
         * 발송은 로그인 없이 열려 있어 같은 주소로 메일을 퍼부을 수 있다.
         * 수신 주소마다 재발송 간격을 둔다.
         */
        limitSend(redisKey)

        val verifyCode = makeRandomCode()

        val context = Context().apply {

            setVariable("serviceName", serviceName)
            setVariable("userEmail", email)
            setVariable("digits", verifyCode)
            setVariable("expireMinutes", 3)
        }

        val content = templateEngine.process("email/verification-code", context)
        val sendMessage = javaMailSender.createMimeMessage()

        try {

            val mimeMessageHelper = MimeMessageHelper(sendMessage, true, "utf-8")
            mimeMessageHelper.setFrom(serviceName, "잇다")
            mimeMessageHelper.setTo(email)
            mimeMessageHelper.setSubject("잇다")
            mimeMessageHelper.setText(content, true)
            javaMailSender.send(sendMessage)
        } catch (_: MessagingException) {

            redisConfig.redisTemplate().delete(sendCooldownKey(redisKey))

            throw ApplicationException(EmailException.EMAIL_CANNOT_SEND)
        } catch (_: UnsupportedEncodingException) {

            redisConfig.redisTemplate().delete(sendCooldownKey(redisKey))

            throw ApplicationException(EmailException.EMAIL_CANNOT_SEND)
        }

        redisConfig.redisTemplate()
            .opsForValue()
            .set(
                redisKey,
                verifyCode,
                3,
                TimeUnit.MINUTES
            )

        redisConfig.redisTemplate().delete(attemptKey(redisKey))

        return SendVerifyCodeResponse.of("${email}로 인증코드를 전송했습니다.")
    }

    /**
     * 틀린 코드를 무제한으로 받으면 TTL 안에 대입해 볼 수 있다.
     * 비교보다 먼저 횟수를 올려야 동시에 몰린 요청도 한도 이상 비교되지 않는다.
     * 한도에 닿으면 인증코드를 폐기해 다시 발송받게 한다.
     */
    private fun checkAndConsume(code: String,
                                redisKey: String): CheckVerifyCodeResponse {

        val redisTemplate = redisConfig.redisTemplate()
        val savedCode = redisTemplate.opsForValue().get(redisKey)
            ?: throw ApplicationException(EmailException.EMAIL_CANNOT_VERIFY)

        val attempts = redisTemplate.opsForValue().increment(attemptKey(redisKey))
            ?: throw ApplicationException(EmailException.EMAIL_CANNOT_VERIFY)

        if (attempts == 1L) {

            redisTemplate.expire(
                attemptKey(redisKey),
                3,
                TimeUnit.MINUTES
            )
        }

        if (attempts > MAX_VERIFY_ATTEMPTS) {

            redisTemplate.delete(
                listOf(
                    redisKey,
                    attemptKey(redisKey)
                )
            )

            throw ApplicationException(EmailException.EMAIL_VERIFY_ATTEMPTS_EXCEEDED)
        }

        if (savedCode != code) {

            throw ApplicationException(EmailException.EMAIL_CANNOT_VERIFY)
        }

        redisTemplate.delete(
            listOf(
                redisKey,
                attemptKey(redisKey)
            )
        )

        return CheckVerifyCodeResponse.of("이메일이 인증되었습니다.")
    }

    private fun limitSend(redisKey: String) {

        val acquired = redisConfig.redisTemplate()
            .opsForValue()
            .setIfAbsent(
                sendCooldownKey(redisKey),
                "1",
                SEND_COOLDOWN_SECONDS,
                TimeUnit.SECONDS
            )
            ?: false

        if (!acquired) {

            throw ApplicationException(EmailException.EMAIL_SEND_TOO_FREQUENT)
        }
    }

    private fun signupKey(email: String): String = "email:signup:$email"

    private fun changeEmailKey(email: String): String = "email:change:$email"

    private fun verifiedKey(email: String): String = "email:verified:${email.trim().lowercase()}"

    private fun attemptKey(redisKey: String): String = "$redisKey:attempts"

    private fun sendCooldownKey(redisKey: String): String = "$redisKey:send:cooldown"

    companion object {

        private const val MAX_VERIFY_ATTEMPTS = 5L

        private const val SEND_COOLDOWN_SECONDS = 10L

        private const val VERIFIED_MARK_MINUTES = 30L
    }
}
