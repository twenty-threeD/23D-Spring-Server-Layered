package spring.springserver.domain.community.like.service

import spring.springserver.domain.community.like.data.request.CommunityPostLikeRequest
import spring.springserver.domain.community.like.data.response.CommunityLikeResponse

interface CommunityLikeService {

    fun likePost(
        communityPostLikeRequest: CommunityPostLikeRequest
    ): CommunityLikeResponse

    fun unlikePost(
        postId: Long
    ): CommunityLikeResponse
}
