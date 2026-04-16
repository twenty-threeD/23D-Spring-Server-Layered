package spring.springserver.domain.file.data.request;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record FileUploadRequest(

        @NotNull
        MultipartFile file
) {
}
