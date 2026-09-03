package spring.springserver.domain.community.post.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.community.comment.repository.CommunityCommentRepository
import spring.springserver.domain.community.common.data.response.DeleteResponse
import spring.springserver.domain.community.common.service.CommunityAuthorizationService
import spring.springserver.domain.community.like.repository.CommunityPostLikeRepository
import spring.springserver.domain.community.post.data.request.CreatePostRequest
import spring.springserver.domain.community.post.data.request.UpdatePostRequest
import spring.springserver.domain.community.post.data.response.CommunityPostResponse
import spring.springserver.domain.community.post.data.response.CreatePostResponse
import spring.springserver.domain.community.post.data.response.UpdatePostResponse
import spring.springserver.domain.community.post.entity.Category
import spring.springserver.domain.community.post.entity.CommunityPost
import spring.springserver.domain.community.post.repository.CommunityPostRepository
import spring.springserver.domain.community.post.service.CommunityPostService
import spring.springserver.domain.profile.service.ProfileService
import java.time.LocalDateTime

@Service
@Transactional(rollbackFor = [Exception::class])
class CommunityPostServiceImpl(
    private val communityPostRepository: CommunityPostRepository,
    private val communityCommentRepository: CommunityCommentRepository,
    private val communityPostLikeRepository: CommunityPostLikeRepository,
    private val communityAuthorizationService: CommunityAuthorizationService,
    private val profileService: ProfileService,
): CommunityPostService {

    override fun createPost(
        createPostRequest: CreatePostRequest
    ): CreatePostResponse {

        val member = communityAuthorizationService.getCurrentMember()

        val communityPost = communityPostRepository.save(
            createPostRequest.toEntity(member)
        )

        return CreatePostResponse.of(communityPost)
    }

    override fun updatePost(
        updatePostRequest: UpdatePostRequest
    ): UpdatePostResponse {

        val member = communityAuthorizationService.getCurrentMember()

        val communityPost = communityAuthorizationService
            .getActivePost(updatePostRequest.postId)

        communityAuthorizationService.validateOwner(member, communityPost.member.getId())

        communityPost.update(
            title = updatePostRequest.title.trim(),
            category = updatePostRequest.category,
            content = updatePostRequest.content?.trim()?.takeIf { it.isNotBlank() },
            fileUrl = updatePostRequest.fileUrl?.trim()?.takeIf { it.isNotBlank() },
        )

        return UpdatePostResponse.of(communityPost)
    }

    override fun deletePost(
        postId: Long
    ): DeleteResponse {

        val member = communityAuthorizationService.getCurrentMember()

        val communityPost = communityAuthorizationService.getActivePost(postId)

        communityAuthorizationService.validateOwner(
            member,
            communityPost.member.getId()
        )

        communityPost.softDelete(LocalDateTime.now())

        return DeleteResponse.of("삭제되었습니다.")
    }

    @Transactional(readOnly = true)
    override fun getPosts(): List<CommunityPostResponse> {

        return communityPostRepository
            .findAllByDeletedAtIsNullOrderByUpdatedAtDesc()
            .map {
                
                communityPost ->
                CommunityPostResponse.toPostResponse(
                    communityPost,
                    communityCommentRepository,
                    communityPostLikeRepository
                )
            }
    }

    @Transactional(readOnly = true)
    override fun getPost(
        postId: Long
    ): CommunityPostResponse {

        val communityPost = communityAuthorizationService.getActivePost(postId)

        communityPost.increaseViewCount()

        return CommunityPostResponse.toPostResponse(
            communityPost,
            communityCommentRepository,
            communityPostLikeRepository,
            getImageUrl(communityPost)
        )
    }

    @Transactional(readOnly = true)
    override fun searchPosts(
        keyword: String
    ): List<CommunityPostResponse> {

        val normalizedKeyword = keyword.trim()

        return toResponses(communityPostRepository.searchPosts(normalizedKeyword))
    }

    @Transactional(readOnly = true)
    override fun searchPostsByCategory(
        category: Category
    ): List<CommunityPostResponse> {

        return toResponses(communityPostRepository.searchPostsByCategory(category))
    }

    /**
     * 목록은 회원별 프로필 이미지를 한 번에 조회해 게시글마다 조회하지 않도록 한다.
     */
    private fun toResponses(
        communityPosts: List<CommunityPost>
    ): List<CommunityPostResponse> {

        val imageUrls = profileService.getImageUrlsByMemberIds(
            communityPosts.mapNotNull { communityPost -> communityPost.member.getId() }
        )

        return communityPosts.map { communityPost ->

            CommunityPostResponse.toPostResponse(
                communityPost,
                communityCommentRepository,
                communityPostLikeRepository,
                imageUrls[communityPost.member.getId()]
            )
        }
    }

    private fun getImageUrl(
        communityPost: CommunityPost
    ): String? {

        return communityPost.member.getId()
            ?.let { memberId -> profileService.getImageUrlsByMemberIds(listOf(memberId))[memberId] }
    }
}