package spring.springserver.domain.auth.data.response;

public record SignUpResponse(

        String message
) {

    public static SignUpResponse of(String message) {

        return new SignUpResponse(message);
    }
}