package spring.springserver.domain.notification.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import spring.springserver.domain.notification.entity.Notification

/**
 * 읽은 알림은 삭제하므로, 테이블에 남아 있는 알림이 곧 읽지 않은 알림이다.
 */
interface NotificationRepository : JpaRepository<Notification, Long> {

    @Query(
        """
            select n
            from Notification n
            join fetch n.receiver
            left join fetch n.sender
            where n.receiver.username = :username
            order by n.sentAt desc, n.id desc
            """
    )
    fun findAllByReceiverUsername(
        @Param("username") username: String
    ): List<Notification>

    /**
     * 재접속 시 놓친 알림을 다시 보내기 위한 조회.
     * Last-Event-ID 이후에 생긴, 즉 아직 전달되지 못한 알림만 가져온다.
     */
    @Query(
        """
            select n
            from Notification n
            join fetch n.receiver
            left join fetch n.sender
            where n.receiver.username = :username
              and n.id > :lastEventId
            order by n.sentAt asc, n.id asc
            """
    )
    fun findMissedByReceiverUsername(
        @Param("username") username: String,
        @Param("lastEventId") lastEventId: Long
    ): List<Notification>

    @Query(
        """
            select count(n)
            from Notification n
            where n.receiver.username = :username
            """
    )
    fun countByReceiverUsername(
        @Param("username") username: String
    ): Long

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
            delete from Notification n
            where n.receiver.username = :username
            """
    )
    fun deleteAllByReceiverUsername(
        @Param("username") username: String
    ): Int
}