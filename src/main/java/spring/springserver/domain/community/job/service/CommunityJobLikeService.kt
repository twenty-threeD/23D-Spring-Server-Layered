package spring.springserver.domain.community.job.service

import spring.springserver.domain.community.job.data.request.JobPostLikeRequest
import spring.springserver.domain.community.job.data.response.JobPostLikeResponse

interface CommunityJobLikeService {

    fun likeJobPost(
        jobPostLikeRequest: JobPostLikeRequest
    ): JobPostLikeResponse

    fun unlikeJobPost(
        postId: Long
    ): JobPostLikeResponse
}
