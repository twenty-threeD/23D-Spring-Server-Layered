package spring.springserver.domain.file.controller

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import spring.springserver.domain.file.data.request.FileUploadRequest
import spring.springserver.domain.file.data.response.FileUploadResponse
import spring.springserver.domain.file.service.FileService
import spring.springserver.global.data.BaseResponse

@RestController
@RequestMapping("/api/files")
class FileController(
    private val fileService: FileService
) {

    @PostMapping("/upload")
    fun uploadFile(
        @ModelAttribute @Valid fileUploadRequest: FileUploadRequest
    ): BaseResponse<FileUploadResponse> {

        return BaseResponse.ok(fileService.uploadFile(fileUploadRequest))
    }
}
