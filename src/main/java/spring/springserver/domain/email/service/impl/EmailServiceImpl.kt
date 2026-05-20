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

        return (1..8).map { chars[SecureRandom().nextInt(chars.length)] }.joinToString("")
    }

    override fun sendVerifyCode(email: String): SendVerifyCodeResponse {

        val verifyCode = makeRandomCode()

        val context = Context().apply {

            setVariable("serviceName", serviceName)
            setVariable("userEmail", email)
            setVariable("digits", verifyCode.map { it.toString() })
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
        } catch (e: MessagingException) {

            throw ApplicationException(EmailException.EMAIL_CANNOT_SEND)
        } catch (e: UnsupportedEncodingException) {

            throw ApplicationException(EmailException.EMAIL_CANNOT_SEND)
        }

        redisConfig.redisTemplate()
            .opsForValue()
            .set(
                email,
                verifyCode,
                3,
                TimeUnit.MINUTES
            )

        return SendVerifyCodeResponse.of("${email}로 인증코드를 전송했습니다.")
    }

    override fun checkVerifyCode(email: String,
                                 code: String): CheckVerifyCodeResponse {

        val savedCode = redisConfig.redisTemplate().opsForValue().get(email)

        if (savedCode == code) {

            redisConfig.redisTemplate().delete(email)

            return CheckVerifyCodeResponse.of("이메일이 인증되었습니다.")
        }

        throw ApplicationException(EmailException.EMAIL_CANNOT_VERIFY)
    }
}
