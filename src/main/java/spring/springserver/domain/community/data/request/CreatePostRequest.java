package spring.springserver.domain.community.data.request;

import jakarta.validation.constraints.NotBlank;
import spring.springserver.domain.community.entity.CommunityPost;
import spring.springserver.domain.member.entity.Member;

public record CreatePostRequest(

        @NotBlank
        String title,

        String content,

        String imageUrl
) {

    public CommunityPost toEntity(Member member) {

        return CommunityPost.builder()
                .member(member)
                .username(member.getUsername())
                .title(title)
                .content(content)
                .imageUrl(imageUrl)
                .viewCount(0)
                .build();
    }
}
