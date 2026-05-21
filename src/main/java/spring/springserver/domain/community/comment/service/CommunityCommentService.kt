package spring.springserver.domain.community.comment.service

import spring.springserver.domain.community.comment.data.request.CreateCommentRequest
import spring.springserver.domain.community.comment.data.request.UpdateCommentRequest
import spring.springserver.domain.community.comment.data.response.CommunityCommentResponse
import spring.springserver.domain.community.global.data.response.DeleteResponse
import spring.springserver.domain.community.like.data.request.CommunityCommentLikeRequest
import spring.springserver.domain.community.like.data.response.CommunityLikeResponse

interface CommunityCommentService {

    fun createComment(createCommentRequest: CreateCommentRequest): CommunityCommentResponse

    fun getComments(postId: Long): List<CommunityCommentResponse>

    fun updateComment(updateCommentRequest: UpdateCommentRequest): CommunityCommentResponse

    fun deleteComment(commentId: Long): DeleteResponse
}
