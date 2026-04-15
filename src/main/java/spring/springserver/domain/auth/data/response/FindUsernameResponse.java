package spring.springserver.domain.auth.data.response;

public record FindUsernameResponse(

        String message,

        String username
) {
    public static FindUsernameResponse of(String message, String username) {

        return new FindUsernameResponse(message, username);
    }
}
