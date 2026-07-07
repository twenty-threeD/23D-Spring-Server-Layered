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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private static final Set<String> ALLOWED_MIME = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "application/pdf"
    );

    private static final Set<String> ALLOWED_EXT = Set.of(
            "jpg",
            "jpeg",
            "png",
            "webp",
            "pdf"
    );

    private final Tika tika = new Tika();

    @Value("${app.upload.file-dir}")
    private String fileDirectory;

    public FileUploadResponse uploadFile(FileUploadRequest fileUploadRequest) {

        MultipartFile multipartFile = fileUploadRequest.multipartFile();

        if (multipartFile == null || multipartFile.isEmpty()) {

            throw new ApplicationException(FileStatusCode.FILE_EMPTY);
        }

        try {

            String detectedType;

            try (InputStream inputStream = multipartFile.getInputStream()) {

                detectedType = tika.detect(inputStream);
            }

            if (!ALLOWED_MIME.contains(detectedType)) {

                throw new ApplicationException(FileStatusCode.FILE_UPLOAD_FAILED);
            }

            String originalFilename = multipartFile.getOriginalFilename();

            if (originalFilename == null || !originalFilename.contains(".")) {

                throw new ApplicationException(FileStatusCode.FILE_UPLOAD_FAILED);
            }

            String ext = originalFilename.substring(
                    originalFilename.lastIndexOf('.') + 1)
                    .toLowerCase();

            if (!ALLOWED_EXT.contains(ext)) {

                throw new ApplicationException(FileStatusCode.FILE_UPLOAD_FAILED);
            }

            Path uploadPath = Path.of(fileDirectory)
                    .toAbsolutePath()
                    .normalize();

            Files.createDirectories(uploadPath);

            String storedFileName = UUID.randomUUID() + "." + ext;

            Path targetPath = uploadPath
                    .resolve(storedFileName)
                    .normalize();

            if (!targetPath.startsWith(uploadPath)) {

                throw new ApplicationException(FileStatusCode.FILE_UPLOAD_FAILED);
            }

            try (InputStream inputStream = multipartFile.getInputStream()) {

                Files.copy(
                        inputStream,
                        targetPath,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

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

    public void deleteFile(String fileUrl) {

        if (fileUrl == null || fileUrl.isBlank()) {

            return;
        }

        try {

            Path uploadPath = Path.of(fileDirectory)
                    .toAbsolutePath()
                    .normalize();

            Path fileName = Path.of(fileUrl).getFileName();

            if (fileName == null) {

                return;
            }

            Path targetPath = uploadPath
                    .resolve(fileName.toString())
                    .normalize();

            if (!targetPath.startsWith(uploadPath)) {

                throw new ApplicationException(FileStatusCode.FILE_UPLOAD_FAILED);
            }

            Files.deleteIfExists(targetPath);

        } catch (IOException ioException) {

            throw new ApplicationException(
                    FileStatusCode.FILE_UPLOAD_FAILED,
                    ioException
            );
        }
    }
}
