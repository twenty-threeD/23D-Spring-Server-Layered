package spring.springserver.domain.file.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import spring.springserver.domain.file.data.request.FileUploadRequest;
import spring.springserver.domain.file.data.response.FileUploadResponse;
import spring.springserver.domain.file.exception.FileStatusCode;
import spring.springserver.global.exception.exception.ApplicationException;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    @Value("${app.upload.file-dir}")
    private String fileDirectory;

    private static final Set<String> ALLOWED_EXT = Set.of(
            "jpg", "jpeg", "png", "gif", "pdf"
    );

    public FileUploadResponse uploadFile(FileUploadRequest fileUploadRequest) {

        MultipartFile multipartFile = fileUploadRequest.multipartFile();

        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new ApplicationException(FileStatusCode.FILE_EMPTY);
        }

        try {

            Path uploadPath = Path.of(fileDirectory).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String originalFilename = multipartFile.getOriginalFilename();

            String ext = StringUtils.getFilenameExtension(originalFilename);
            if (ext == null || !ALLOWED_EXT.contains(ext.toLowerCase())) {
                throw new ApplicationException(FileStatusCode.FILE_UPLOAD_FAILED);
            }

            String storedFileName = UUID.randomUUID().toString() + "." + ext;

            Path targetPath = uploadPath.resolve(storedFileName).normalize();

            if (!targetPath.startsWith(uploadPath)) {
                throw new ApplicationException(FileStatusCode.FILE_UPLOAD_FAILED);
            }

            Files.copy(multipartFile.getInputStream(), targetPath);

            return FileUploadResponse.of(
                    "/files/" + storedFileName,
                    "파일 업로드가 완료되었습니다."
            );

        } catch (IOException e) {
            
            throw new ApplicationException(FileStatusCode.FILE_UPLOAD_FAILED, e);
        }
    }
}