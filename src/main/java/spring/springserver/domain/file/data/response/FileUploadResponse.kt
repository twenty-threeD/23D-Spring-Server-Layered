package spring.springserver.domain.file.data.response

data class FileUploadResponse(
    val fileUrl: String,
    val message: String
) {

    companion object {

        fun of(fileUrl: String, message: String): FileUploadResponse {
            return FileUploadResponse(
                fileUrl,
                message
            )
        }
    }
}
