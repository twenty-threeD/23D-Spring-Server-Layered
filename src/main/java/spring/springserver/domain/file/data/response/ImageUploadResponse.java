package spring.springserver.domain.file.data.response;

public record ImageUploadResponse(

        String imageUrl,

        String message
) {

    public static ImageUploadResponse of(String imageUrl,
                                         String message) {

        return new ImageUploadResponse(imageUrl,
                                       message);
    }
}
