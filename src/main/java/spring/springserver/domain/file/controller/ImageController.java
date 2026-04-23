package spring.springserver.domain.file.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.springserver.domain.file.data.request.ImageUploadRequest;
import spring.springserver.domain.file.data.response.ImageUploadResponse;
import spring.springserver.domain.file.service.ImageService;
import spring.springserver.global.data.BaseResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/images")
public class ImageController {

    private final ImageService imageService;

    @PostMapping("/upload")
    public BaseResponse<ImageUploadResponse> uploadImage(

            @ModelAttribute @Valid final ImageUploadRequest imageUploadRequest) {

        return BaseResponse.ok(imageService.uploadImage(imageUploadRequest));
    }
}
