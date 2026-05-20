package spring.springserver.domain.community.post.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.community.comment.repository.CommunityCommentRepository
import spring.springserver.domain.community.post.data.request.CreatePostRequest
import spring.springserver.domain.community.post.data.request.UpdatePostRequest
import spring.springserver.domain.community.post.data.response.CommunityPostResponse
import spring.springserver.domain.community.post.data.response.CreatePostResponse
import spring.springserver.domain.community.post.data.response.UpdatePostResponse
import spring.springserver.domain.community.post.entity.CommunityPost
import spring.springserver.domain.community.post.repository.CommunityPostRepository
import spring.springserver.domain.community.global.service.CommunityAuthorizationService
import java.time.LocalDateTime

@Service
@Transactional
class CommunityPostServiceImpl(private val communityPostRepository: CommunityPostRepository,
                                    private val communityCommentRepository: CommunityCommentRepository,
                                    private val communityAuthorizationService: CommunityAuthorizationService) : CommunityPostService {

    override fun createPost(createPostRequest: CreatePostRequest): CreatePostResponse {

        val member = communityAuthorizationService.getCurrentMember()

        val communityPost = communityPostRepository.save(createPostRequest.toEntity(member))

        return CreatePostResponse.of(communityPost)
    }

    override fun updatePost(updatePostRequest: UpdatePostRequest): UpdatePostResponse {

        val member = communityAuthorizationService.getCurrentMember()

        val communityPost = communityAuthorizationService
            .getActivePost(updatePostRequest.postId)

        communityAuthorizationService.validateOwner(member, communityPost.member.getId())

        communityPost.update(
            title = updatePostRequest.title.trim(),
            content = updatePostRequest.content?.trim()?.takeIf { it.isNotBlank() },
            fileUrl = updatePostRequest.fileUrl?.trim()?.takeIf { it.isNotBlank() },
        )

        return UpdatePostResponse.of(communityPost)
    }

    override fun deletePost(postId: Long) {

        val member = communityAuthorizationService.getCurrentMember()

        val communityPost = communityAuthorizationService.getActivePost(postId)

        communityAuthorizationService.validateOwner(
            member,
            communityPost.member.getId()
        )

        communityPost.softDelete(LocalDateTime.now())
    }

    override fun getPost(postId: Long): CommunityPostResponse {

        val communityPost = communityAuthorizationService.getActivePost(postId)

        communityPost.increaseViewCount()

        return toPostResponse(communityPost)
    }

    @Transactional(readOnly = true)
    override fun searchPosts(keyword: String): List<CommunityPostResponse> {

        val normalizedKeyword = keyword.trim()

        return communityPostRepository.searchPosts(normalizedKeyword)
            .map(::toPostResponse)
    }

    private fun toPostResponse(communityPost: CommunityPost): CommunityPostResponse {

        val postId = communityPost.getId()!!

        return CommunityPostResponse.of(
            communityPost = communityPost,
            commentCount = communityCommentRepository.countByCommunityPostIdAndDeletedAtIsNull(postId),
        )
    }
}
