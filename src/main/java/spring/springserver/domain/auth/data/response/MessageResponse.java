package spring.springserver.domain.auth.data.response;

public record MessageResponse(

        String message
) {
    public static MessageResponse of(String message) {

        return new MessageResponse(message);
    }
}
