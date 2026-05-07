package spring.springserver.domain.file.service;

import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import spring.springserver.domain.file.data.request.FileUploadRequest;
import spring.springserver.domain.file.data.response.FileUploadResponse;
import spring.springserver.domain.file.exception.FileStatusCode;
import spring.springserver.global.exception.exception.ApplicationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    @Value("${app.upload.file-dir}")
    private String fileDirectory;

    private static final Set<String> ALLOWED_MIME = Set.of(
            "files/jpg",
            "files/jpeg",
            "files/png",
            "files/pdf"
    );

    private final Tika tika = new Tika();

    public FileUploadResponse uploadFile(FileUploadRequest fileUploadRequest) {

        MultipartFile multipartFile = fileUploadRequest.multipartFile();

        if (multipartFile == null || multipartFile.isEmpty()) {

            throw new ApplicationException(FileStatusCode.FILE_EMPTY);
        }

        try {

            Path uploadPath = Path.of(fileDirectory).toAbsolutePath().normalize();

            Files.createDirectories(uploadPath);

            String detectedType;

            try (InputStream inputStream = multipartFile.getInputStream()) {

                detectedType = tika.detect(inputStream);
            }

            if (!ALLOWED_MIME.contains(detectedType)) {

                throw new ApplicationException(FileStatusCode.FILE_UPLOAD_FAILED);
            }

            String originalFilename = multipartFile.getOriginalFilename();

            String ext = originalFilename.substring(
                    originalFilename.lastIndexOf('.') + 1
            );

            String storedFileName = UUID.randomUUID() + "." + ext;

            Path targetPath = uploadPath.resolve(storedFileName).normalize();

            if (!targetPath.startsWith(uploadPath)) {

                throw new ApplicationException(FileStatusCode.FILE_UPLOAD_FAILED);
            }

            Files.copy(
                    multipartFile.getInputStream(),
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return FileUploadResponse.of(
                    "/files/" + storedFileName,
                    "파일 업로드가 완료되었습니다."
            );

        } catch (IOException ioException) {

            throw new ApplicationException(
                    FileStatusCode.FILE_UPLOAD_FAILED,
                    ioException
            );
        }
    }
}