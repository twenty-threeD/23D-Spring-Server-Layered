package spring.springserver.domain.auth.data.response;

public record SignOutResponse(

        String message
) {

    public static SignOutResponse of(String message) {

        return new SignOutResponse(message);
    }
}
