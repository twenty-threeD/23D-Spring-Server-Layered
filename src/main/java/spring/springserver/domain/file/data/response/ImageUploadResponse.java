package spring.springserver.domain.file.data.response;

public record ImageUploadResponse(

        String image_url,

        String message
) {
    public static ImageUploadResponse of(
            String image_url,
            String message) {

        return new ImageUploadResponse(
                image_url,
                message);
    }
}
