package spring.springserver.domain.file.data.response;

public record FileUploadResponse(

        String fileUrl,

        String message
) {

    public static FileUploadResponse of(String fileUrl,
                                        String message) {

        return new FileUploadResponse(fileUrl,
                                      message);
    }
}