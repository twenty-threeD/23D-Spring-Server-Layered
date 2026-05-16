package spring.springserver.domain.community.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.community.data.request.CreatePostRequest
import spring.springserver.domain.community.data.request.UpdatePostRequest
import spring.springserver.domain.community.data.response.CommunityPostResponse
import spring.springserver.domain.community.data.response.CreatePostResponse
import spring.springserver.domain.community.data.response.UpdatePostResponse
import spring.springserver.domain.community.entity.CommunityPost
import spring.springserver.domain.community.repository.CommunityCommentRepository
import spring.springserver.domain.community.repository.CommunityPostRepository
import java.time.LocalDateTime

@Service
@Transactional
class CommunityPostServiceImpl(
    private val communityPostRepository: CommunityPostRepository,
    private val communityCommentRepository: CommunityCommentRepository,
    private val communityAccessSupport: CommunityAccessSupport,
) : CommunityPostService {

    override fun createPost(createPostRequest: CreatePostRequest): CreatePostResponse {
        val member = communityAccessSupport.getCurrentMember()
        val communityPost = communityPostRepository.save(createPostRequest.toEntity(member))
        return CreatePostResponse.of(communityPost)
    }

    override fun updatePost(updatePostRequest: UpdatePostRequest): UpdatePostResponse {
        val member = communityAccessSupport.getCurrentMember()
        val communityPost = communityAccessSupport.getActivePost(updatePostRequest.postId)
        communityAccessSupport.validateOwner(member, communityPost.member.getId())

        communityPost.update(
            title = updatePostRequest.title.trim(),
            content = updatePostRequest.content?.trim()?.takeIf { it.isNotBlank() },
            fileUrl = updatePostRequest.fileUrl?.trim()?.takeIf { it.isNotBlank() },
        )

        return UpdatePostResponse.of(communityPost)
    }

    override fun deletePost(postId: Long) {
        val member = communityAccessSupport.getCurrentMember()
        val communityPost = communityAccessSupport.getActivePost(postId)
        communityAccessSupport.validateOwner(member, communityPost.member.getId())
        communityPost.softDelete(LocalDateTime.now())
    }

    override fun getPost(postId: Long): CommunityPostResponse {
        val communityPost = communityAccessSupport.getActivePost(postId)
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
