package spring.springserver.domain.community.job.service

import spring.springserver.domain.community.common.data.response.DeleteResponse
import spring.springserver.domain.community.job.data.request.CreateJobCommentRequest
import spring.springserver.domain.community.job.data.request.UpdateJobCommentRequest
import spring.springserver.domain.community.job.data.response.CommunityJobCommentResponse

interface CommunityJobCommentService {

    fun createJobComment(
        createJobCommentRequest: CreateJobCommentRequest
    ): CommunityJobCommentResponse

    fun getJobComments(
        postId: Long
    ): List<CommunityJobCommentResponse>

    fun updateJobComment(
        updateJobCommentRequest: UpdateJobCommentRequest
    ): CommunityJobCommentResponse

    fun deleteJobComment(
        commentId: Long
    ): DeleteResponse
}
