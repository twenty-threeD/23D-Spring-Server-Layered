package spring.springserver.domain.community.job.event

import spring.springserver.domain.community.post.entity.CommunityPostType

/**
 * 알림 리스너가 커밋 이후에 동작하므로, 지연 로딩 대상을 넘기지 않고
 * 다시 조회할 수 있는 식별자만 담는다.
 */
data class JobPostCreatedEvent(
    val postId: Long,
    val writerMemberId: Long,
    val postType: CommunityPostType,
    val title: String,
    val jobCategoryId: Long,
    val sigCd: String
)
