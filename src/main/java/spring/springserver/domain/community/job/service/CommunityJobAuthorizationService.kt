package spring.springserver.domain.community.job.service

import org.springframework.stereotype.Component
import spring.springserver.domain.community.job.entity.CommunityJobComment
import spring.springserver.domain.community.job.entity.CommunityJobPost
import spring.springserver.domain.community.job.repository.CommunityJobCommentRepository
import spring.springserver.domain.community.job.repository.CommunityJobPostRepository
import spring.springserver.global.exception.exception.ApplicationException
import spring.springserver.global.exception.status_code.CommonStatusCode

/**
 * 구인/구직 게시판의 조회 가드.
 * 회원 조회·작성자 검증은 게시판을 가리지 않으므로 CommunityAuthorizationService를 그대로 쓴다.
 */
@Component
class CommunityJobAuthorizationService(
    private val communityJobPostRepository: CommunityJobPostRepository,
    private val communityJobCommentRepository: CommunityJobCommentRepository
) {

    fun getActiveJobPost(
        postId: Long
    ): CommunityJobPost {

        return communityJobPostRepository.findByIdAndDeletedAtIsNull(postId)
            ?: throw ApplicationException.of(
                CommonStatusCode.ENDPOINT_NOT_FOUND,
                "존재하지 않는 구인/구직 게시글입니다."
            )
    }

    fun getActiveJobComment(
        commentId: Long
    ): CommunityJobComment {

        return communityJobCommentRepository.findByIdAndDeletedAtIsNull(commentId)
            ?: throw ApplicationException.of(
                CommonStatusCode.ENDPOINT_NOT_FOUND,
                "존재하지 않는 댓글입니다."
            )
    }
}
