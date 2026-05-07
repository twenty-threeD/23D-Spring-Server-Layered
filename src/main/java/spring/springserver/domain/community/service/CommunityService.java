package spring.springserver.domain.community.service;

import spring.springserver.domain.community.data.request.CreatePostRequest;
import spring.springserver.domain.community.data.response.CommunityPostResponse;
import spring.springserver.domain.community.data.response.CreatePostResponse;

import java.util.List;

public interface CommunityService {

    CreatePostResponse createPost(CreatePostRequest createPostRequest);

    List<CommunityPostResponse> getPostsByTitle(String title);

    List<CommunityPostResponse> getPostsByUsername(String username);
}
