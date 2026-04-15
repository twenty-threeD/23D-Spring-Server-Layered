package spring.springserver.domain.member.data.response;

public record DeleteAccountResponse(

        String message
) {

    public static DeleteAccountResponse of(String message) {

        return new DeleteAccountResponse(message);
    }
}
