package spring.springserver.domain.community.job.service.impl

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.community.common.data.response.DeleteResponse
import spring.springserver.domain.community.common.service.CommunityAuthorizationService
import spring.springserver.domain.community.job.data.request.CreateJobCommentRequest
import spring.springserver.domain.community.job.data.request.UpdateJobCommentRequest
import spring.springserver.domain.community.job.data.response.CommunityJobCommentResponse
import spring.springserver.global.data.PageResponse
import spring.springserver.domain.community.job.entity.CommunityJobComment
import spring.springserver.domain.community.job.repository.CommunityJobCommentRepository
import spring.springserver.domain.community.job.service.CommunityJobAuthorizationService
import spring.springserver.domain.community.job.service.CommunityJobCommentService
import java.time.LocalDateTime

@Service
@Transactional(rollbackFor = [Exception::class])
class CommunityJobCommentServiceImpl(
    private val communityJobCommentRepository: CommunityJobCommentRepository,
    private val communityAuthorizationService: CommunityAuthorizationService,
    private val communityJobAuthorizationService: CommunityJobAuthorizationService
): CommunityJobCommentService {

    override fun createJobComment(
        createJobCommentRequest: CreateJobCommentRequest
    ): CommunityJobCommentResponse {

        val member = communityAuthorizationService.getCurrentMember()

        val communityJobPost = communityJobAuthorizationService
            .getActiveJobPost(createJobCommentRequest.postId!!)

        val communityJobComment = communityJobCommentRepository.save(
            CommunityJobComment(
                member = member,
                communityJobPost = communityJobPost,
                content = createJobCommentRequest.content!!.trim(),
                isEdited = false,
            )
        )

        return CommunityJobCommentResponse.of(
            communityJobComment = communityJobComment,
        )
    }

    @Transactional(readOnly = true)
    override fun getJobComments(
        postId: Long,
        page: Int,
        size: Int
    ): PageResponse<CommunityJobCommentResponse> {

        communityJobAuthorizationService.getActiveJobPost(postId)

        val communityJobComments = communityJobCommentRepository
            .findAllByCommunityJobPostIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(
                postId,
                PageRequest.of(page, size)
            )

        return PageResponse.of(
            communityJobComments,
            communityJobComments.content.map {

                communityJobComment ->
                CommunityJobCommentResponse.of(
                    communityJobComment = communityJobComment,
                )
            }
        )
    }

    override fun updateJobComment(
        updateJobCommentRequest: UpdateJobCommentRequest
    ): CommunityJobCommentResponse {

        val member = communityAuthorizationService.getCurrentMember()

        val communityJobComment = communityJobAuthorizationService
            .getActiveJobComment(updateJobCommentRequest.commentId!!)

        communityAuthorizationService.validateOwner(
            member,
            communityJobComment.member.getId()
        )

        communityJobComment.update(updateJobCommentRequest.content!!.trim())

        return CommunityJobCommentResponse.of(
            communityJobComment = communityJobComment,
        )
    }

    override fun deleteJobComment(
        commentId: Long
    ): DeleteResponse {

        val member = communityAuthorizationService.getCurrentMember()

        val communityJobComment = communityJobAuthorizationService.getActiveJobComment(commentId)

        communityAuthorizationService.validateOwner(
            member,
            communityJobComment.member.getId()
        )

        communityJobComment.softDelete(LocalDateTime.now())

        return DeleteResponse.of("삭제되었습니다.")
    }
}
