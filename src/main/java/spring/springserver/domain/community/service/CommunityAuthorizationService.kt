package spring.springserver.domain.community.service

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import spring.springserver.domain.auth.exception.AuthStatusCode
import spring.springserver.domain.community.entity.CommunityComment
import spring.springserver.domain.community.entity.CommunityPost
import spring.springserver.domain.community.repository.CommunityCommentRepository
import spring.springserver.domain.community.repository.CommunityPostRepository
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.global.exception.exception.ApplicationException
import spring.springserver.global.exception.status_code.CommonStatusCode

@Component
class CommunityAuthorizationService(
    private val communityPostRepository: CommunityPostRepository,
    private val communityCommentRepository: CommunityCommentRepository,
    private val memberRepository: MemberRepository,
) {

    fun getCurrentMember(): Member {

        val authentication = SecurityContextHolder.getContext().authentication

        val username = authentication?.name

        if (username.isNullOrBlank() || username == "anonymousUser") {

            throw ApplicationException(AuthStatusCode.INVALID_JWT)
        }

        return memberRepository.findByUsername(username)
            ?: throw ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)
    }

    fun getActivePost(postId: Long): CommunityPost {

        return communityPostRepository.findByIdAndDeletedAtIsNull(postId)
            ?: throw ApplicationException.of(
                CommonStatusCode.ENDPOINT_NOT_FOUND,
                "존재하지 않는 게시글입니다."
            )
    }

    fun getActiveComment(commentId: Long): CommunityComment {

        return communityCommentRepository.findByIdAndDeletedAtIsNull(commentId)
            ?: throw ApplicationException.of(
                CommonStatusCode.ENDPOINT_NOT_FOUND,
                "존재하지 않는 댓글입니다."
            )
    }

    fun validateOwner(member: Member, ownerId: Long?) {

        if (ownerId == null || member.getId() != ownerId) {

            throw ApplicationException.of(
                CommonStatusCode.INVALID_ARGUMENT,
                "작성자만 수정하거나 삭제할 수 있습니다."
            )
        }
    }
}
