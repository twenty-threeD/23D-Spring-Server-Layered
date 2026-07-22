package spring.springserver.domain.file.service

import org.apache.tika.Tika
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import spring.springserver.domain.file.data.request.FileUploadRequest
import spring.springserver.domain.file.data.response.FileUploadResponse
import spring.springserver.domain.file.exception.FileStatusCode
import spring.springserver.global.exception.exception.ApplicationException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID

@Service
class FileService(
    @Value("\${app.upload.file-dir}")
    private val fileDirectory: String
) {

    private val tika = Tika()

    fun uploadFile(
            fileUploadRequest: FileUploadRequest
        ): FileUploadResponse {

        val multipartFile = fileUploadRequest.multipartFile

        if (multipartFile == null || multipartFile.isEmpty) {

            throw ApplicationException(FileStatusCode.FILE_EMPTY)
        }

        try {

            val detectedType = multipartFile.inputStream.use {
                inputStream -> tika.detect(inputStream)
            }

            if (detectedType !in ALLOWED_MIME) {

                throw ApplicationException(FileStatusCode.FILE_UPLOAD_FAILED)
            }

            val originalFilename = multipartFile.originalFilename

            if (originalFilename == null || !originalFilename.contains(".")) {

                throw ApplicationException(FileStatusCode.FILE_UPLOAD_FAILED)
            }

            val ext = originalFilename
                .substring(originalFilename.lastIndexOf('.') + 1)
                .lowercase()

            if (ext !in ALLOWED_EXT) {

                throw ApplicationException(FileStatusCode.FILE_UPLOAD_FAILED)
            }

            val uploadPath = Path.of(fileDirectory)
                .toAbsolutePath()
                .normalize()

            Files.createDirectories(uploadPath)

            val storedFileName = "${UUID.randomUUID()}.$ext"

            val targetPath = uploadPath
                .resolve(storedFileName)
                .normalize()

            if (!targetPath.startsWith(uploadPath)) {

                throw ApplicationException(FileStatusCode.FILE_UPLOAD_FAILED)
            }

            multipartFile.inputStream.use {

                inputStream -> Files.copy(
                    inputStream,
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
                )
            }

            return FileUploadResponse.of(
                "/files/$storedFileName",
                "파일 업로드가 완료되었습니다."
            )
        } catch (ioException: IOException) {

            throw ApplicationException(
                FileStatusCode.FILE_UPLOAD_FAILED,
                ioException
            )
        }
    }

    fun deleteFile(fileUrl: String?) {

        if (fileUrl.isNullOrBlank()) {

            return
        }

        try {

            val uploadPath = Path.of(fileDirectory)
                .toAbsolutePath()
                .normalize()

            val fileName = Path.of(fileUrl).fileName ?: return

            val targetPath = uploadPath
                .resolve(fileName.toString())
                .normalize()

            if (!targetPath.startsWith(uploadPath)) {

                throw ApplicationException(FileStatusCode.FILE_UPLOAD_FAILED)
            }

            Files.deleteIfExists(targetPath)
        } catch (ioException: IOException) {

            throw ApplicationException(
                FileStatusCode.FILE_UPLOAD_FAILED,
                ioException
            )
        }
    }

    companion object {

        private val ALLOWED_MIME = setOf(
            "image/jpeg",
            "image/png",
            "image/webp",
            "application/pdf"
        )

        private val ALLOWED_EXT = setOf(
            "jpg",
            "jpeg",
            "png",
            "webp",
            "pdf"
        )
    }
}
