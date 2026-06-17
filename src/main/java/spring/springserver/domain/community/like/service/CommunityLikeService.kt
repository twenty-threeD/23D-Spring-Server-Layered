package spring.springserver.domain.community.like.service

import spring.springserver.domain.community.like.data.request.CommunityCommentLikeRequest
import spring.springserver.domain.community.like.data.response.CommunityLikeResponse

interface CommunityLikeService {

    fun likeComment(
        communityCommentLikeRequest: CommunityCommentLikeRequest
    ): CommunityLikeResponse

    fun unlikeComment(
        commentId: Long
    ): CommunityLikeResponse
}