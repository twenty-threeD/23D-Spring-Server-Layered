package spring.springserver.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.file-dir}")
    private String fileDir;

    @Value("${app.upload.image-dir}")
    private String imageDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + Path.of(fileDir).toAbsolutePath().normalize() + "/");

        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + Path.of(imageDir).toAbsolutePath().normalize() + "/");
    }
}
