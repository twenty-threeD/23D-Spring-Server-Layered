package spring.springserver.domain.call.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import spring.springserver.domain.call.entity.Call
import spring.springserver.domain.call.entity.CallStatus
import java.time.Instant

interface CallRepository : JpaRepository<Call, Long> {

    @Query(
        """
            select c
            from Call c
            join fetch c.caller
            join fetch c.callee
            join fetch c.room
            where c.id = :callId
            """
    )
    fun findByIdWithParticipants(
        @Param("callId") callId: Long
    ): Call?

    @Query(
        """
            select count(c) > 0
            from Call c
            where c.status in :statuses
              and (c.caller.id in :memberIds or c.callee.id in :memberIds)
            """
    )
    fun existsByStatusInAndParticipantIn(
        @Param("statuses") statuses: Collection<CallStatus>,
        @Param("memberIds") memberIds: Collection<Long>
    ): Boolean

    @Query(
        """
            select c
            from Call c
            join fetch c.caller
            join fetch c.callee
            join fetch c.room
            where c.status = :status
              and c.createdAt < :threshold
            """
    )
    fun findAllByStatusAndCreatedAtBefore(
        @Param("status") status: CallStatus,
        @Param("threshold") threshold: Instant
    ): List<Call>

    @Query(
        """
            select c
            from Call c
            join fetch c.caller
            join fetch c.callee
            join fetch c.room
            where c.status = :status
              and c.startedAt < :threshold
            """
    )
    fun findAllByStatusAndStartedAtBefore(
        @Param("status") status: CallStatus,
        @Param("threshold") threshold: Instant
    ): List<Call>

    @Query(
        """
            select c
            from Call c
            join fetch c.caller
            join fetch c.callee
            join fetch c.room
            where c.room.id = :roomId
            order by c.id desc
            """
    )
    fun findAllByRoomId(
        @Param("roomId") roomId: Long
    ): List<Call>
}
