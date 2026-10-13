package spring.springserver.domain.post.review.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import spring.springserver.domain.post.review.entity.PostReviewAnalysis

@Repository
interface PostReviewAnalysisRepository: JpaRepository<PostReviewAnalysis, Long> {

    fun findByRevieweeId(
        revieweeId: Long
    ): PostReviewAnalysis?
}
