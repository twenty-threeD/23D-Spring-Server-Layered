package spring.springserver.domain.community.comment.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.community.comment.data.request.CreateCommentRequest
import spring.springserver.domain.community.comment.data.request.UpdateCommentRequest
import spring.springserver.domain.community.comment.data.response.CommunityCommentResponse
import spring.springserver.domain.community.comment.entity.CommunityComment
import spring.springserver.domain.community.comment.repository.CommunityCommentRepository
import spring.springserver.domain.community.comment.service.CommunityCommentService
import spring.springserver.domain.community.common.data.response.DeleteResponse
import spring.springserver.domain.community.common.service.CommunityAuthorizationService
import spring.springserver.domain.profile.service.ProfileService
import java.time.LocalDateTime

@Service
@Transactional(rollbackFor = [Exception::class])
class CommunityCommentServiceImpl(
    private val communityCommentRepository: CommunityCommentRepository,
    private val communityAuthorizationService: CommunityAuthorizationService,
    private val profileService: ProfileService
): CommunityCommentService {

    override fun createComment(
        createCommentRequest: CreateCommentRequest
    ): CommunityCommentResponse {

        val member = communityAuthorizationService.getCurrentMember()

        val communityPost = communityAuthorizationService.getActivePost(createCommentRequest.postId)

        val communityComment = communityCommentRepository.save(
            CommunityComment(
                member = member,
                communityPost = communityPost,
                content = createCommentRequest.content.trim(),
                isEdited = false,
            )
        )

        return CommunityCommentResponse.of(
            communityComment = communityComment,
            likeCount = 0L,
            imageUrl = getImageUrl(communityComment)
        )
    }

    @Transactional(readOnly = true)
    override fun getComments(
        postId : Long
    ): List<CommunityCommentResponse> {

        communityAuthorizationService.getActivePost(postId)

        val communityComments = communityCommentRepository
            .findAllByCommunityPostIdAndDeletedAtIsNullOrderByCreatedAtDesc(postId)

        val imageUrls = profileService.getImageUrlsByMemberIds(
            communityComments.mapNotNull { communityComment -> communityComment.member.getId() }
        )

        return communityComments.map {
                communityComment ->
                CommunityCommentResponse.of(
                    communityComment = communityComment,
                    likeCount = 0L,
                    imageUrl = imageUrls[communityComment.member.getId()]
                )
            }
    }

    override fun updateComment(
        updateCommentRequest: UpdateCommentRequest
    ): CommunityCommentResponse {

        val member = communityAuthorizationService.getCurrentMember()

        val communityComment = communityAuthorizationService.getActiveComment(updateCommentRequest.commentId)

        communityAuthorizationService.validateOwner(
            member,
            communityComment.member.getId()
        )

        communityComment.update(updateCommentRequest.content.trim())

        return CommunityCommentResponse.of(
            communityComment = communityComment,
            likeCount = 0L,
            imageUrl = getImageUrl(communityComment)
        )
    }

    override fun deleteComment(
        commentId: Long
    ): DeleteResponse {

        val member = communityAuthorizationService.getCurrentMember()

        val communityComment = communityAuthorizationService.getActiveComment(commentId)

        communityAuthorizationService.validateOwner(
            member,
            communityComment.member.getId()
        )

        communityComment.softDelete(LocalDateTime.now())

        return DeleteResponse.of("삭제되었습니다.")
    }

    private fun getImageUrl(
        communityComment: CommunityComment
    ): String? {

        return communityComment.member.getId()
            ?.let { memberId -> profileService.getImageUrlsByMemberIds(listOf(memberId))[memberId] }
    }
}