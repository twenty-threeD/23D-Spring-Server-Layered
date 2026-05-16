package spring.springserver.domain.community.service.comment

import spring.springserver.domain.community.data.request.CommunityCommentLikeRequest
import spring.springserver.domain.community.data.request.CreateCommentRequest
import spring.springserver.domain.community.data.request.UpdateCommentRequest
import spring.springserver.domain.community.data.response.CommunityCommentResponse
import spring.springserver.domain.community.data.response.CommunityLikeResponse

interface CommunityCommentService {

    fun createComment(createCommentRequest: CreateCommentRequest): CommunityCommentResponse

    fun getComments(postId: Long): List<CommunityCommentResponse>

    fun updateComment(updateCommentRequest: UpdateCommentRequest): CommunityCommentResponse

    fun deleteComment(commentId: Long)

    fun likeComment(communityCommentLikeRequest: CommunityCommentLikeRequest): CommunityLikeResponse

    fun unlikeComment(commentId: Long): CommunityLikeResponse
}
