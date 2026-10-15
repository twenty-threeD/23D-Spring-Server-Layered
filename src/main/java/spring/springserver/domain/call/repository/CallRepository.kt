package spring.springserver.domain.call.repository

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import spring.springserver.domain.call.entity.Call
import spring.springserver.domain.call.entity.CallStatus
import java.time.Instant

interface CallRepository : JpaRepository<Call, Long> {

    /**
     * 상태를 바꾸기 전에 통화 행을 잠근다(SELECT ... FOR UPDATE).
     * 동시에 들어온 수락·취소·부재중 처리가 같은 RINGING을 보고 둘 다 통과하지 않도록,
     * 나중 요청은 앞 요청이 커밋될 때까지 기다렸다가 바뀐 상태를 다시 읽는다.
     * 잠금 대상을 통화 행으로 한정하려고 연관 엔티티는 fetch join 하지 않는다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Call c where c.id = :callId")
    fun findByIdForUpdate(
        @Param("callId") callId: Long
    ): Call?

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

    /**
     * 회원이 참여 중인 진행 중 통화. 동시 통화를 막으므로 정상이라면 많아야 한 건이다.
     */
    @Query(
        """
            select c
            from Call c
            join fetch c.caller
            join fetch c.callee
            join fetch c.room
            where c.status in :statuses
              and (c.caller.id = :memberId or c.callee.id = :memberId)
            order by c.id desc
            """
    )
    fun findAllByStatusInAndParticipant(
        @Param("statuses") statuses: Collection<CallStatus>,
        @Param("memberId") memberId: Long
    ): List<Call>

    /**
     * 잠그기 전에 대상만 고르는 용도라 id만 가져온다.
     * 엔티티를 먼저 읽어 두면 같은 영속성 컨텍스트에서 [findByIdForUpdate]가
     * 행은 잠그되 이미 올라온 오래된 상태를 그대로 돌려준다.
     */
    @Query(
        """
            select c.id
            from Call c
            where c.status in :statuses
              and (c.caller.id = :memberId or c.callee.id = :memberId)
            order by c.id desc
            """
    )
    fun findIdsByStatusInAndParticipant(
        @Param("statuses") statuses: Collection<CallStatus>,
        @Param("memberId") memberId: Long
    ): List<Long>
}
