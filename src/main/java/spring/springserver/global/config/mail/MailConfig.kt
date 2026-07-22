package spring.springserver.global.config.mail

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.thymeleaf.TemplateEngine
import spring.springserver.domain.email.service.impl.EmailServiceImpl
import spring.springserver.global.config.redis.RedisConfig
import java.util.Properties

@Configuration
class MailConfig(
    @Value("\${spring.mail.username}")
    private val username: String,

    @Value("\${spring.mail.password}")
    private val password: String,

    @Value("\${spring.mail.host}")
    private val host: String
) {

    @Bean
    fun emailService(
        javaMailSender: JavaMailSender,
        redisConfig: RedisConfig,
        templateEngine: TemplateEngine
    ): EmailServiceImpl {

        return EmailServiceImpl(
            javaMailSender,
            redisConfig,
            templateEngine
        )
    }

    @Bean
    fun mailSender(): JavaMailSender {

        return JavaMailSenderImpl().apply {

            host = this@MailConfig.host
            port = 587
            username = this@MailConfig.username
            password = this@MailConfig.password
            javaMailProperties = Properties().apply {

                put("mail.transport.protocol", "smtp")
                put("mail.debug", "true")
                put("mail.smtp.ssl.trust", "smtp.gmail.com")
                put("mail.smtp.ssl.protocols", "TLSv1.3")
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
            }
        }
    }
}
