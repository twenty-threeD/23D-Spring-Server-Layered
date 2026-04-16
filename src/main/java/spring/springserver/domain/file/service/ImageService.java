package spring.springserver.domain.file.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import spring.springserver.domain.file.data.request.ImageUploadRequest;
import spring.springserver.domain.file.data.response.ImageUploadResponse;
import spring.springserver.domain.file.exception.FileStatusCode;
import spring.springserver.global.exception.exception.ApplicationException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class ImageService {

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    @Value("${app.upload.image-dir}")
    private String imageDir;

    public ImageUploadResponse uploadImage(ImageUploadRequest imageUploadRequest) {

        MultipartFile file = imageUploadRequest.file();

        if (file == null || file.isEmpty()) {
            throw new ApplicationException(FileStatusCode.FILE_EMPTY);
        }

        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new ApplicationException(FileStatusCode.INVALID_IMAGE_TYPE);
        }

        try {
            Path uploadPath = Path.of(imageDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String originalFilename = file.getOriginalFilename();
            String storedFileName = UUID.randomUUID() + "_" + originalFilename;
            Path targetPath = uploadPath.resolve(storedFileName);

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            return ImageUploadResponse.of(
                    "/images/" + storedFileName,
                    "이미지 업로드가 완료되었습니다."
            );
        } catch (IOException e) {
            throw new ApplicationException(FileStatusCode.FILE_UPLOAD_FAILED, e);
        }
    }
}
