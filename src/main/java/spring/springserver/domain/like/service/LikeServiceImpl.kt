package spring.springserver.domain.like.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import spring.springserver.domain.like.data.response.LikeResponse
import spring.springserver.domain.like.data.response.UnlikeResponse
import spring.springserver.domain.like.entity.Like
import spring.springserver.domain.like.exception.LikeStatusCode
import spring.springserver.domain.like.repository.LikeRepository
import spring.springserver.domain.like.service.usecase.LikeUseCase
import spring.springserver.domain.like.service.usecase.UnLikeUseCase
import spring.springserver.domain.member.exception.MemberStatusCode
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.global.exception.exception.ApplicationException

@Service
@Transactional
class LikeServiceImpl(private val likeRepository: LikeRepository,
                      private val memberRepository: MemberRepository): LikeUseCase, UnLikeUseCase {

    // 좋아요
    override fun like(memberId: Long): LikeResponse {

        val member = memberRepository.findMemberById(memberId)
            ?: throw ApplicationException(MemberStatusCode.MEMBER_NOT_FOUND)

        if (likeRepository.existsByMember(member)) {

            throw ApplicationException(LikeStatusCode.ALREADY_LIKED)
        }

        likeRepository.save(
            Like(
                member = member
            )
        )

        return LikeResponse.of("좋아요를 보냈습니다.")
    }

    override fun unlike(memberId: Long): UnlikeResponse {

        likeRepository.delete(likeRepository.findByMember(memberRepository.findMemberById(memberId)
            ?: throw ApplicationException(MemberStatusCode.MEMBER_NOT_FOUND)
        )
            ?: throw ApplicationException(LikeStatusCode.NOT_LIKED))

        return UnlikeResponse.of("좋아요를 취소했습니다.")
    }
}