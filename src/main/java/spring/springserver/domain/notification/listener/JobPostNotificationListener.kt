package spring.springserver.domain.notification.listener

import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import spring.springserver.domain.community.job.event.JobPostCreatedEvent
import spring.springserver.domain.community.job.service.CommunityJobPostService
import spring.springserver.domain.jobcategory.service.JobCategoryService
import spring.springserver.domain.location.service.LocationService
import spring.springserver.domain.notification.service.NotificationService
import spring.springserver.domain.profile.repository.ProfileRepository

/**
 * 구인/구직 글이 올라오면 글쓴이 지역에서 반경 안에 있고
 * 프로필 카테고리가 글의 카테고리와 맞는 회원에게 알림을 보낸다.
 *
 * 알림 전송이 실패해도 글 작성은 살아 있어야 하므로 커밋 이후에 동작한다.
 */
@Component
class JobPostNotificationListener(
    private val locationService: LocationService,
    private val jobCategoryService: JobCategoryService,
    private val profileRepository: ProfileRepository,
    private val notificationService: NotificationService
) {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun sendJobPostNotification(
        event: JobPostCreatedEvent
    ) {

        val sigCds = locationService.findNearbySigCds(
            event.sigCd,
            CommunityJobPostService.NEARBY_RADIUS_KM
        )

        /**
         * 상위 카테고리로 올라온 글은 그 아래 카테고리를 보고 있는 회원에게도 알린다.
         */
        val jobCategoryIds = jobCategoryService
            .getCategoryIdsIncludingDescendants(event.jobCategoryId)

        if (sigCds.isEmpty() || jobCategoryIds.isEmpty()) {

            return
        }

        val receiverUsernames = profileRepository.findUsernamesBySigCdsAndJobCategoryIds(
            sigCds,
            jobCategoryIds,
            event.writerMemberId
        )

        if (receiverUsernames.isEmpty()) {

            return
        }

        notificationService.sendJobPostNotification(
            receiverUsernames = receiverUsernames,
            message = "내 주변에 새 ${event.postType.getLabel()} 글이 올라왔습니다. (${event.title})",
            postId = event.postId
        )
    }
}
