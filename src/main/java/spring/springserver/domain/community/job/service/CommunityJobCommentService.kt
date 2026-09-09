package spring.springserver.domain.community.job.service

import spring.springserver.domain.community.common.data.response.DeleteResponse
import spring.springserver.domain.community.job.data.request.CreateJobCommentRequest
import spring.springserver.domain.community.job.data.request.UpdateJobCommentRequest
import spring.springserver.domain.community.job.data.response.CommunityJobCommentResponse
import spring.springserver.global.data.PageResponse

interface CommunityJobCommentService {

    fun createJobComment(
        createJobCommentRequest: CreateJobCommentRequest
    ): CommunityJobCommentResponse

    fun getJobComments(
        postId: Long,
        page: Int,
        size: Int
    ): PageResponse<CommunityJobCommentResponse>

    fun updateJobComment(
        updateJobCommentRequest: UpdateJobCommentRequest
    ): CommunityJobCommentResponse

    fun deleteJobComment(
        commentId: Long
    ): DeleteResponse
}
