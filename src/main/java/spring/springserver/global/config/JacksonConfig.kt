package spring.springserver.global.config

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * LocalDateTime.now()로 만든 시각은 서버 시간대(운영 컨테이너는 UTC) 기준인데, 기본 직렬화는 시간대 표시 없이 내려간다.
 * 브라우저는 시간대 없는 시각을 자기 시간대(KST)로 읽으므로 모든 시각이 9시간 어긋나 보였다.
 * 서버 시간대 기준 시각으로 보고 UTC로 바꿔 Z를 붙여 내려, 어느 시간대의 클라이언트도 같은 순간으로 읽게 한다.
 *
 * 사용자가 고른 달력 날짜(계약 시작·종료일 등)는 순간이 아니므로 필드에 기본 LocalDateTimeSerializer를 지정해 그대로 둔다.
 */
@Configuration
class JacksonConfig {

    @Bean
    fun utcLocalDateTimeCustomizer(): Jackson2ObjectMapperBuilderCustomizer {

        return Jackson2ObjectMapperBuilderCustomizer { builder ->

            builder.serializerByType(
                LocalDateTime::class.java,
                UtcLocalDateTimeSerializer()
            )
        }
    }

    private class UtcLocalDateTimeSerializer: StdSerializer<LocalDateTime>(LocalDateTime::class.java) {

        override fun serialize(
            value: LocalDateTime,
            jsonGenerator: JsonGenerator,
            serializerProvider: SerializerProvider
        ) {

            jsonGenerator.writeString(
                value.atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toString()
            )
        }
    }
}