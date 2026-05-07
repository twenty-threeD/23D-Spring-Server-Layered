package spring.springserver.domain.community.data.response;


import java.time.LocalDateTime;

public record CreatePostResponse(

        String username,

        String title,

        String content,

        LocalDateTime createAt
) {

    public static CreatePostResponse of(String username,
                                        String title,
                                        String content,
                                        LocalDateTime createAt) {

        return new CreatePostResponse(
                username,
                title,
                content,
                createAt
        );
    }
}
