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
import java.util.regex.Pattern
import java.util.UUID
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Service
class FileService(
    @param:Value($$"${app.upload.file-dir}")
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

            if (detectedType !in ALLOWED_MIME_EXTENSIONS) {

                throw ApplicationException(FileStatusCode.FILE_UPLOAD_FAILED)
            }

            val originalFilename = multipartFile.originalFilename

            if (originalFilename == null || !originalFilename.contains(".")) {

                throw ApplicationException(FileStatusCode.FILE_UPLOAD_FAILED)
            }

            val ext = originalFilename
                .substring(originalFilename.lastIndexOf('.') + 1)
                .lowercase()

            val allowedExtensions = ALLOWED_MIME_EXTENSIONS[detectedType]

            if (allowedExtensions == null || ext !in allowedExtensions) {

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

            registerRollbackCleanup(storedFileName)

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

            val normalizedFileUrl = fileUrl.trim()

            if (!normalizedFileUrl.startsWith("/files/")) {

                throw ApplicationException(FileStatusCode.FILE_UPLOAD_FAILED)
            }

            val fileName = normalizedFileUrl.removePrefix("/files/")

            if (!ALLOWED_STORED_FILE_NAME.matcher(fileName).matches()) {

                throw ApplicationException(FileStatusCode.FILE_UPLOAD_FAILED)
            }

            val uploadPath = Path.of(fileDirectory)
                .toAbsolutePath()
                .normalize()

            val targetPath = uploadPath
                .resolve(fileName)
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

        private val ALLOWED_MIME_EXTENSIONS = mapOf(
            "image/jpeg" to setOf("jpg", "jpeg"),
            "image/png" to setOf("png"),
            "image/webp" to setOf("webp"),
            "application/pdf" to setOf("pdf")
        )

        private val ALLOWED_STORED_FILE_NAME: Pattern =
            Pattern.compile("^[0-9a-fA-F\\-]{36}\\.(jpg|jpeg|png|webp|pdf)$")
    }

    private fun registerRollbackCleanup(storedFileName: String) {

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {

            return
        }

        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {

            override fun afterCompletion(status: Int) {

                if (status != TransactionSynchronization.STATUS_COMMITTED) {

                    runCatching {
                        
                        val uploadPath = Path.of(fileDirectory)
                            .toAbsolutePath()
                            .normalize()

                        Files.deleteIfExists(
                            uploadPath.resolve(storedFileName).normalize()
                        )
                    }
                }
            }
        })
    }
}
