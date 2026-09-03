package spring.springserver.domain.file.data.request

import jakarta.validation.constraints.NotNull
import org.springframework.web.multipart.MultipartFile

data class FileUploadRequest(
    @field:NotNull
    val multipartFile: MultipartFile?
)
