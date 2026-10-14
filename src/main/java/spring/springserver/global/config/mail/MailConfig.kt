package spring.springserver.global.config.mail

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import org.thymeleaf.TemplateEngine
import spring.springserver.domain.email.service.impl.EmailServiceImpl
import spring.springserver.global.config.redis.RedisConfig

@Configuration
class MailConfig {

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
}
