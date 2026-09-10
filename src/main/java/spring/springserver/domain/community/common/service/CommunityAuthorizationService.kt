package spring.springserver.domain.community.common.service

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import spring.springserver.domain.auth.exception.AuthStatusCode
import spring.springserver.domain.community.comment.entity.CommunityComment
import spring.springserver.domain.community.comment.repository.CommunityCommentRepository
import spring.springserver.domain.community.post.entity.CommunityPost
import spring.springserver.domain.community.post.repository.CommunityPostRepository
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.global.exception.exception.ApplicationException
import spring.springserver.global.exception.status_code.CommonStatusCode

@Component
class CommunityAuthorizationService(
    private val communityPostRepository: CommunityPostRepository,
    private val communityCommentRepository: CommunityCommentRepository,
    private val memberRepository: MemberRepository
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

    /**
     * 비로그인이면 null이다. permitAll로 열린 조회에서 로그인 여부에 따라
     * 응답을 달리할 때 쓴다. 예외를 잡아 삼키면 DB 장애까지 비로그인으로 보이므로
     * 인증 여부만 보고 갈라낸다.
     */
    fun getCurrentMemberOrNull(): Member? {

        val username = SecurityContextHolder.getContext().authentication?.name

        if (username.isNullOrBlank() || username == "anonymousUser") {

            return null
        }

        return memberRepository.findByUsername(username)
    }

    fun getActivePost(
        postId: Long
    ): CommunityPost {

        return communityPostRepository.findByIdAndDeletedAtIsNull(postId)
            ?: throw ApplicationException.of(
                CommonStatusCode.ENDPOINT_NOT_FOUND,
                "존재하지 않는 게시글입니다."
            )
    }

    fun getActiveComment(
        commentId: Long
    ): CommunityComment {

        return communityCommentRepository.findByIdAndDeletedAtIsNull(commentId)
            ?: throw ApplicationException.of(
                CommonStatusCode.ENDPOINT_NOT_FOUND,
                "존재하지 않는 댓글입니다."
            )
    }

    fun validateOwner(
        member: Member,
        ownerId: Long?
    ) {

        if (ownerId == null || member.getId() != ownerId) {

            throw ApplicationException.of(
                CommonStatusCode.INVALID_ARGUMENT,
                "작성자만 수정하거나 삭제할 수 있습니다."
            )
        }
    }
}