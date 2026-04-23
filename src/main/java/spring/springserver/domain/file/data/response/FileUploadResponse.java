package spring.springserver.domain.file.data.response;

public record FileUploadResponse(

        String file_url,

        String message
) {
    public static FileUploadResponse of(
            String file_url,
            String message) {

        return new FileUploadResponse(
                file_url,
                message);
    }
}