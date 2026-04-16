package spring.springserver.domain.file.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import spring.springserver.domain.file.data.request.FileUploadRequest;
import spring.springserver.domain.file.data.response.FileUploadResponse;
import spring.springserver.domain.file.exception.FileStatusCode;
import spring.springserver.global.exception.exception.ApplicationException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class FileService {

    @Value("${app.upload.file-dir}")
    private String fileDir;

    public FileUploadResponse uploadFile(FileUploadRequest fileUploadRequest) {

        MultipartFile file = fileUploadRequest.file();

        if (file == null || file.isEmpty()) {
            throw new ApplicationException(FileStatusCode.FILE_EMPTY);
        }

        try {
            Path uploadPath = Path.of(fileDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String originalFilename = file.getOriginalFilename();
            String storedFileName = UUID.randomUUID() + "_" + originalFilename;
            Path targetPath = uploadPath.resolve(storedFileName);

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            return FileUploadResponse.of(
                    "/files/" + storedFileName,
                    "파일 업로드가 완료되었습니다."
            );
        } catch (IOException e) {
            throw new ApplicationException(FileStatusCode.FILE_UPLOAD_FAILED, e);
        }
    }
}
