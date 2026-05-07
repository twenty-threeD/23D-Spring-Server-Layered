package spring.springserver.domain.file.service;

import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import spring.springserver.domain.file.data.request.ImageUploadRequest;
import spring.springserver.domain.file.data.response.ImageUploadResponse;
import spring.springserver.domain.file.exception.FileStatusCode;
import spring.springserver.global.exception.exception.ApplicationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService {

    private static final Set<String> ALLOWED_MIME = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final Tika tika = new Tika();

    @Value("${app.upload.image-dir}")
    private String imageDirectory;

    public ImageUploadResponse uploadImage(ImageUploadRequest imageUploadRequest) {

        MultipartFile multipartFile = imageUploadRequest.multipartFile();

        if (multipartFile == null || multipartFile.isEmpty()) {

            throw new ApplicationException(FileStatusCode.FILE_EMPTY);
        }

        try {

            String detectedType;

            try (InputStream inputStream = multipartFile.getInputStream()) {

                detectedType = tika.detect(inputStream);
            }

            if (!ALLOWED_MIME.contains(detectedType)) {

                throw new ApplicationException(FileStatusCode.INVALID_IMAGE_TYPE);
            }

            Path uploadPath = Path.of(imageDirectory)
                    .toAbsolutePath()
                    .normalize();

            Files.createDirectories(uploadPath);

            String originalFilename = multipartFile.getOriginalFilename();

            String storedFileName =
                    UUID.randomUUID() + "_" + originalFilename;

            Path targetPath = uploadPath
                    .resolve(storedFileName)
                    .normalize();

            if (!targetPath.startsWith(uploadPath)) {

                throw new ApplicationException(
                        FileStatusCode.FILE_UPLOAD_FAILED
                );
            }

            Files.copy(
                    multipartFile.getInputStream(),
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return ImageUploadResponse.of(
                    "/images/" + storedFileName,
                    "이미지 업로드가 완료되었습니다."
            );

        } catch (IOException e) {

            throw new ApplicationException(
                    FileStatusCode.FILE_UPLOAD_FAILED,
                    e
            );
        }
    }
}