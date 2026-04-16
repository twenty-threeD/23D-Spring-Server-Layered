package spring.springserver.domain.member.data.response;

public record PasswordResetResponse(

        String message
) {

    public static PasswordResetResponse of(String message) {

        return new PasswordResetResponse(message);
    }
}