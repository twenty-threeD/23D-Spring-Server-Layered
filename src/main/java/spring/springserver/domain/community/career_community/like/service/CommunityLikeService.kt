package spring.springserver.domain.community.career_community.like.service

import spring.springserver.domain.community.community.like.data.request.CommunityPostLikeRequest
import spring.springserver.domain.community.community.like.data.response.CommunityLikeResponse

interface CommunityLikeService {

    fun likePost(
        communityPostLikeRequest: CommunityPostLikeRequest
    ): CommunityLikeResponse

    fun unlikePost(
        postId: Long
    ): CommunityLikeResponse
}