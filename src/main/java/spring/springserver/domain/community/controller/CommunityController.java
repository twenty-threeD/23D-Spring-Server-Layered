package spring.springserver.domain.community.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import spring.springserver.domain.community.data.request.CreatePostRequest;
import spring.springserver.domain.community.data.response.CommunityPostResponse;
import spring.springserver.domain.community.data.response.CreatePostResponse;
import spring.springserver.domain.community.service.CommunityService;
import spring.springserver.global.data.BaseResponse;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community")
public class CommunityController {

    private final CommunityService communityService;

    @PostMapping("/post")
    public BaseResponse<CreatePostResponse> createPost(@RequestBody @Valid final CreatePostRequest createPostRequest){

        return BaseResponse.ok(communityService.createPost(createPostRequest));
    }

    @GetMapping("/search")
    public BaseResponse<List<CommunityPostResponse>> searchPosts(@RequestParam String keyword){

        return BaseResponse.ok(communityService.searchPosts(keyword));
    }
}
