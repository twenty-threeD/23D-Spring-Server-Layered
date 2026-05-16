package spring.springserver.domain.like.post.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import spring.springserver.domain.like.post.data.response.PostLikeResponse
import spring.springserver.domain.like.post.data.response.PostUnlikeResponse
import spring.springserver.domain.like.post.entity.PostLike
import spring.springserver.domain.like.exception.LikeStatusCode
import spring.springserver.domain.like.post.data.request.PostLikeRequest
import spring.springserver.domain.like.post.data.request.PostUnlikeRequest
import spring.springserver.domain.like.post.repository.PostLikeRepository
import spring.springserver.domain.like.post.service.usecase.PostLikeUseCase
import spring.springserver.domain.like.post.service.usecase.PostUnLikeUseCase
import spring.springserver.domain.member.exception.MemberStatusCode
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.global.exception.exception.ApplicationException

@Service
@Transactional
class PostLikeImpl(private val likeRepository: PostLikeRepository,
                   private val memberRepository: MemberRepository):
    PostLikeUseCase,
    PostUnLikeUseCase {

    // 좋아요
    override fun like(postLikeRequest: PostLikeRequest): PostLikeResponse {

        val member = memberRepository.findMemberById(postLikeRequest.memberId)
            ?: throw ApplicationException(MemberStatusCode.MEMBER_NOT_FOUND)

        if (likeRepository.existsByMember(member)) {

            throw ApplicationException(LikeStatusCode.ALREADY_LIKED)
        }

        likeRepository.save(
            PostLike(
                member = member
            )
        )

        return PostLikeResponse.of("좋아요를 보냈습니다.")
    }

    override fun unlike(postUnlikeRequest: PostUnlikeRequest): PostUnlikeResponse {

        likeRepository.delete(likeRepository.findByMember(memberRepository.findMemberById(postUnlikeRequest.memberId)
            ?: throw ApplicationException(MemberStatusCode.MEMBER_NOT_FOUND)
        )
            ?: throw ApplicationException(LikeStatusCode.NOT_LIKED)
        )

        return PostUnlikeResponse.of("좋아요를 취소했습니다.")
    }
}