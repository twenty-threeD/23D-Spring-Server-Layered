package spring.springserver.domain.community.service;


import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.springserver.domain.community.data.request.CreatePostRequest;
import spring.springserver.domain.community.data.response.CommunityPostResponse;
import spring.springserver.domain.community.data.response.CreatePostResponse;
import spring.springserver.domain.community.entity.CommunityPost;
import spring.springserver.domain.community.repository.CommunityPostRepository;
import spring.springserver.domain.auth.exception.AuthStatusCode;
import spring.springserver.domain.member.entity.Member;
import spring.springserver.domain.member.repository.MemberRepository;
import spring.springserver.global.exception.exception.ApplicationException;

import java.util.List;
import java.util.Optional;


@Service
@Transactional
@RequiredArgsConstructor
public class CommunityServiceImpl implements CommunityService {

    private final CommunityPostRepository communityPostRepository;
    private final MemberRepository memberRepository;

    @Override
    public CreatePostResponse createPost(CreatePostRequest createPostRequest) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {

            throw new ApplicationException(AuthStatusCode.INVALID_JWT);
        }

        Member member = Optional.ofNullable(memberRepository.findByUsername(authentication.getName()))
                .orElseThrow(
                        () -> new ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)
                );

        CommunityPost communityPost = communityPostRepository.save(createPostRequest.toEntity(member));

        return CreatePostResponse.of(
                communityPost.getUsername(),
                communityPost.getTitle(),
                communityPost.getContent(),
                communityPost.getCreatedAt()
        );
    }

    @Override
    public List<CommunityPostResponse> getPostsByTitle(String title){

        return communityPostRepository.findByTitleLike(title)
                .stream()
                .map(CommunityPostResponse::from)
                .toList();
    }

    @Override
    public List<CommunityPostResponse> getPostsByUsername(String username){

        return communityPostRepository.findByUsernameLike(username)
                .stream()
                .map(CommunityPostResponse::from)
                .toList();
    }
}
