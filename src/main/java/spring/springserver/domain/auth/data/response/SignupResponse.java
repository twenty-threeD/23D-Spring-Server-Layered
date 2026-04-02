package spring.springserver.domain.auth.data.response;

public record SignupResponse(
        String message
) {

    public static SignupResponse of(String message) {
        return new SignupResponse(message);
    }
}
