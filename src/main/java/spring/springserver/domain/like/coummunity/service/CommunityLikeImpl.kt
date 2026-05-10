package spring.springserver.domain.like.coummunity.service

import org.springframework.stereotype.Service
import spring.springserver.domain.like.coummunity.data.request.CommunityPostLikeRequest
import spring.springserver.domain.like.coummunity.data.request.CommunityPostUnlikeRequest
import spring.springserver.domain.like.coummunity.data.response.CommunityPostLikeResponse
import spring.springserver.domain.like.coummunity.data.response.CommunityPostUnlikeResponse
import spring.springserver.domain.like.coummunity.entity.CommunityLike
import spring.springserver.domain.like.coummunity.repository.CommunityLikeRepository
import spring.springserver.domain.like.coummunity.service.usecase.CommunityPostLikeUseCase
import spring.springserver.domain.like.coummunity.service.usecase.CommunityPostUnlikeUseCase
import spring.springserver.domain.like.exception.LikeStatusCode
import spring.springserver.domain.member.exception.MemberStatusCode
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.global.exception.exception.ApplicationException

@Service
class CommunityLikeImpl(private val communityLikeRepository: CommunityLikeRepository,
                        private val memberRepository: MemberRepository):
    CommunityPostLikeUseCase,
    CommunityPostUnlikeUseCase{

    override fun like(communityLikeRequest: CommunityPostLikeRequest): CommunityPostLikeResponse {

        val member = memberRepository.findMemberById(communityLikeRequest.memberId)
            ?: throw ApplicationException(MemberStatusCode.MEMBER_NOT_FOUND)

        if (communityLikeRepository.existsByMember(member)) {

            throw ApplicationException(LikeStatusCode.ALREADY_LIKED)
        }

        communityLikeRepository.save(
            CommunityLike(
                member = member
            )
        )

        return CommunityPostLikeResponse.of("좋아요를 보냈습니다.")
    }

    override fun unlike(communityPostUnlikeRequest: CommunityPostUnlikeRequest): CommunityPostUnlikeResponse {

        communityLikeRepository.delete(communityLikeRepository.findByMember(memberRepository.findMemberById(communityPostUnlikeRequest.memberId)
            ?: throw ApplicationException(MemberStatusCode.MEMBER_NOT_FOUND)
        )
            ?: throw ApplicationException(LikeStatusCode.NOT_LIKED)
        )

        return CommunityPostUnlikeResponse.of("좋아요를 취소했습니다.")
    }
}