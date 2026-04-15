package spring.springserver.domain.auth.data.response;

public record FindUsernameResponse(

        String username
) {
    public static FindUsernameResponse of(String username) {

        return new FindUsernameResponse(username);
    }
}
