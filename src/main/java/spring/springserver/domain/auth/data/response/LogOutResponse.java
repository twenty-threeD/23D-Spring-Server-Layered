package spring.springserver.domain.auth.data.response;

public record LogOutResponse(

        int code,
        String message
) {
    public static LogOutResponse of(int code, String message){

        return new LogOutResponse(code, message);
    }
}
