package spring.springserver.domain.auth.data.response;

public record PasswordResetResponse(

        String message
) {

    public static PasswordResetResponse of(String message) {

        return new PasswordResetResponse(message);
    }
}
