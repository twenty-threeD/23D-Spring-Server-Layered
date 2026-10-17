package spring.springserver.domain.community.comment.service

import org.springframework.data.domain.Pageable
import spring.springserver.domain.community.comment.data.request.CreateCommentRequest
import spring.springserver.domain.community.comment.data.request.UpdateCommentRequest
import spring.springserver.domain.community.comment.data.response.CommunityCommentResponse
import spring.springserver.domain.community.common.data.response.DeleteResponse
import spring.springserver.global.data.PageResponse

interface CommunityCommentService {

    fun createComment(
        createCommentRequest: CreateCommentRequest
    ): CommunityCommentResponse

    fun getComments(
        postId: Long,
        pageable: Pageable
    ): PageResponse<CommunityCommentResponse>

    fun updateComment(
        updateCommentRequest: UpdateCommentRequest
    ): CommunityCommentResponse

    fun deleteComment(
        commentId: Long
    ): DeleteResponse
}