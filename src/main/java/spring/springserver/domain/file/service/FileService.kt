package spring.springserver.domain.file.service

import org.apache.tika.Tika
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import spring.springserver.domain.file.data.request.FileUploadRequest
import spring.springserver.domain.file.data.response.FileUploadResponse
import spring.springserver.domain.file.exception.FileStatusCode
import spring.springserver.global.exception.exception.ApplicationException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.regex.Pattern

@Service
class FileService(
    @param:Value($$"${app.upload.file-dir}")
    private val fileDirectory: String,
    private val jdbcTemplate: JdbcTemplate
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

    /**
     * 업로드 파일은 누가 올렸는지 기록하지 않아, 남의 공개 이미지 URL을 내 글에 붙였다 빼는 것만으로 지울 수 있었다.
     * 같은 파일을 여러 곳이 함께 쓰기도 하므로, 아직 어디선가 참조 중이면 지우지 않는다.
     * 호출부는 자기 참조를 커밋한 뒤에 부르므로, 남은 참조는 모두 다른 곳의 것이다.
     */
    fun deleteFile(
        fileUrl: String?
    ) {

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

            if (isReferenced(fileName)) {

                return
            }

            Files.deleteIfExists(targetPath)
        } catch (ioException: IOException) {

            throw ApplicationException(
                FileStatusCode.FILE_UPLOAD_FAILED,
                ioException
            )
        }
    }

    /**
     * 파일 URL을 저장하는 모든 컬럼에서 이 파일을 찾는다.
     * 계약서 URL은 도메인을 붙여 저장되기도 하고 채팅 첨부는 여러 URL을 이어 붙여 저장하므로 파일명으로 비교한다.
     * 파일명은 UUID 형식만 통과하므로 LIKE 와일드카드가 섞이지 않는다.
     */
    private fun isReferenced(
        fileName: String
    ): Boolean {

        val pattern = "%/files/$fileName%"

        return jdbcTemplate.queryForObject(
            FILE_REFERENCE_QUERY,
            Boolean::class.java,
            pattern,
            pattern,
            pattern,
            pattern,
            pattern,
            pattern
        ) == true
    }

    companion object {

        private const val FILE_REFERENCE_QUERY = """
            select exists (select 1 from post_attach where file_url like ?)
                or exists (select 1 from profile where image_url like ?)
                or exists (select 1 from community_post where file_url like ?)
                or exists (select 1 from chat_message where attachment_urls like ?)
                or exists (select 1 from contract where contract_url like ?)
                or exists (select 1 from payment where contract_url like ?)
        """

        private val ALLOWED_MIME_EXTENSIONS = mapOf(
            "image/jpeg" to setOf("jpg", "jpeg"),
            "image/png" to setOf("png"),
            "image/webp" to setOf("webp"),
            "application/pdf" to setOf("pdf")
        )

        private val ALLOWED_STORED_FILE_NAME: Pattern =
            Pattern.compile("^[0-9a-fA-F\\-]{36}\\.(jpg|jpeg|png|webp|pdf)$")
    }

    private fun registerRollbackCleanup(
        storedFileName: String
    ) {

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {

            return
        }

        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {

            override fun afterCompletion(
                status: Int
            ) {

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
