package spring.springserver.domain.file.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.springserver.domain.file.data.request.FileUploadRequest;
import spring.springserver.domain.file.data.response.FileUploadResponse;
import spring.springserver.domain.file.service.FileService;
import spring.springserver.global.data.BaseResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    public BaseResponse<FileUploadResponse> uploadFile(
            @ModelAttribute @Valid final FileUploadRequest fileUploadRequest) {

        return BaseResponse.ok(fileService.uploadFile(fileUploadRequest));
    }
}
