package spring.springserver.domain.member.data.response;

public record ChangeUsernameResponse(

        String message
) {

    public static ChangeUsernameResponse of(String message) {

        return new ChangeUsernameResponse(message);
    }
}