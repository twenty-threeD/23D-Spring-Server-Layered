package spring.springserver.global.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.file.Path

@Configuration
class WebConfig(
    @Value("\${app.upload.file-dir}")
    private val fileDir: String
) : WebMvcConfigurer {

    override fun addResourceHandlers(resourceHandlerRegistry: ResourceHandlerRegistry) {
        resourceHandlerRegistry.addResourceHandler("/files/**")
            .addResourceLocations("file:${Path.of(fileDir).toAbsolutePath().normalize()}/")
    }
}
