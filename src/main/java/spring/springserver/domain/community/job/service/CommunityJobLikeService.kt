package spring.springserver.domain.community.job.service

import spring.springserver.domain.community.job.data.request.JobPostLikeRequest
import spring.springserver.domain.community.like.data.response.CommunityLikeResponse

interface CommunityJobLikeService {

    fun likeJobPost(
        jobPostLikeRequest: JobPostLikeRequest
    ): CommunityLikeResponse

    fun unlikeJobPost(
        postId: Long
    ): CommunityLikeResponse
}
