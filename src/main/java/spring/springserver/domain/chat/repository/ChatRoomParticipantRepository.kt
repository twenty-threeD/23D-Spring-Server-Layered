package spring.springserver.domain.chat.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import spring.springserver.domain.chat.entity.ChatRoomParticipant

interface ChatRoomParticipantRepository : JpaRepository<ChatRoomParticipant, Long> {

    @Query(
        """
            select p
            from ChatRoomParticipant p
            join fetch p.room r
            join fetch r.post
            where p.member.username = :username
              and p.visible = true
            order by
                case when r.lastMessageAt is null then 1 else 0 end,
                r.lastMessageAt desc,
                r.id desc
            """
    )
    fun findVisibleParticipantsByUsername(
        @Param("username") username: String
    ): List<ChatRoomParticipant>

    @Query(
        """
            select p
            from ChatRoomParticipant p
            join fetch p.room r
            join fetch r.post
            join fetch p.member
            where r.id = :roomId
              and p.member.username = :username
            """
    )
    fun findByRoomIdAndMemberUsername(
        @Param("roomId") roomId: Long,
        @Param("username") username: String
    ): ChatRoomParticipant?

    @Query(
        """
            select p
            from ChatRoomParticipant p
            join fetch p.member
            where p.room.id = :roomId
            """
    )
    fun findAllByRoomId(
        @Param("roomId") roomId: Long
    ): List<ChatRoomParticipant>

    /**
     * 여러 방의 참여자를 한 번에 읽는다. 방 목록에서 상대방을 찾을 때 N+1을 피하려고 쓴다.
     */
    @Query(
        """
            select p
            from ChatRoomParticipant p
            join fetch p.room
            join fetch p.member
            where p.room.id in :roomIds
            """
    )
    fun findAllByRoomIds(
        @Param("roomIds") roomIds: Collection<Long>
    ): List<ChatRoomParticipant>

    @Query(
        """
            select p
            from ChatRoomParticipant p
            join fetch p.room
            join fetch p.member
            where p.room.id = :roomId
              and p.member.id = :memberId
            """
    )
    fun findByRoomIdAndMemberId(
        @Param("roomId") roomId: Long?,
        @Param("memberId") memberId: Long?
    ): ChatRoomParticipant?

    @Query(
        """
            select p.room.id as roomId, p.member.id as memberId
            from ChatRoomParticipant p
            where p.room.id in :roomIds
            """
    )
    fun findRoomMemberIdsByRoomIds(
        @Param("roomIds") roomIds: Collection<Long>
    ): List<RoomMemberIdProjection>

    @Query(
        """
            select count(p) > 0
            from ChatRoomParticipant p
            where p.room.id = :roomId
              and p.member.id = :memberId
            """
    )
    fun existsByRoomIdAndMemberId(
        @Param("roomId") roomId: Long?,
        @Param("memberId") memberId: Long?
    ): Boolean
}
