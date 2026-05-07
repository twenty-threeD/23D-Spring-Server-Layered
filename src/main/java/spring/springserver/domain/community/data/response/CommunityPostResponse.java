package spring.springserver.domain.community.data.response;

import spring.springserver.domain.community.entity.CommunityPost;

import java.time.LocalDateTime;

public record CommunityPostResponse(

        Long id,

        String username,

        String title,

        String content,

        String imageUrl,

        int viewCount,

        LocalDateTime createdAt,

        LocalDateTime updatedAt
) {

    public static CommunityPostResponse from(CommunityPost communityPost) {

        return new CommunityPostResponse(
                communityPost.getId(),
                communityPost.getUsername(),
                communityPost.getTitle(),
                communityPost.getContent(),
                communityPost.getImageUrl(),
                communityPost.getViewCount(),
                communityPost.getCreatedAt(),
                communityPost.getUpdatedAt()
        );
    }
}
