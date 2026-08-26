package spring.springserver.domain.email.service.impl

import jakarta.mail.MessagingException
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import spring.springserver.domain.email.exception.EmailException
import spring.springserver.domain.email.service.EmailService
import spring.springserver.domain.email.data.response.SendVerifyCodeResponse
import spring.springserver.domain.email.data.response.CheckVerifyCodeResponse
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

    override fun checkVerifyCode(email: String,
                                 code: String): CheckVerifyCodeResponse {

        return checkAndConsume(
            code,
            signupKey(email)
        )
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

            throw ApplicationException(EmailException.EMAIL_CANNOT_SEND)
        } catch (_: UnsupportedEncodingException) {

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

        return SendVerifyCodeResponse.of("${email}로 인증코드를 전송했습니다.")
    }

    private fun checkAndConsume(code: String,
                                redisKey: String): CheckVerifyCodeResponse {

        val savedCode = redisConfig.redisTemplate().opsForValue().get(redisKey)

        if (savedCode == code) {

            redisConfig.redisTemplate().delete(redisKey)

            return CheckVerifyCodeResponse.of("이메일이 인증되었습니다.")
        }

        throw ApplicationException(EmailException.EMAIL_CANNOT_VERIFY)
    }

    private fun signupKey(email: String): String = "email:signup:$email"

    private fun changeEmailKey(email: String): String = "email:change:$email"
}
