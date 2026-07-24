package spring.springserver.global.config.swagger

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Bean
    fun openAPI(): OpenAPI {

        val jwtSchemeName = "JWT"
        val securityRequirement = SecurityRequirement().addList(jwtSchemeName)
        val components = Components()
            .addSecuritySchemes(
                jwtSchemeName,
                SecurityScheme()
                    .name(jwtSchemeName)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
            )

        return OpenAPI()
            .info(
                Info()
                    .title("Spring Server API")
                    .description("Spring Server API 문서")
                    .version("1.0.0")
            )
            .servers(
                listOf(
                    Server()
                        .url("http://13.125.161.66:8080")
                        .description("EC2 Server")
                )
            )
            .addSecurityItem(securityRequirement)
            .components(components)
    }
}
