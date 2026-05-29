package spring.springserver.domain.community.comment.service

import spring.springserver.domain.community.comment.data.request.CreateCommentRequest
import spring.springserver.domain.community.comment.data.request.UpdateCommentRequest
import spring.springserver.domain.community.comment.data.response.CommunityCommentResponse
import spring.springserver.domain.community.comment.entity.CommunityComment
import spring.springserver.domain.community.common.data.response.DeleteResponse

interface CommunityCommentService {

    fun createComment(createCommentRequest: CreateCommentRequest): CommunityCommentResponse

    fun getComments(postId: Long): List<CommunityComment>

    fun updateComment(updateCommentRequest: UpdateCommentRequest): CommunityCommentResponse

    fun deleteComment(commentId: Long): DeleteResponse
}
