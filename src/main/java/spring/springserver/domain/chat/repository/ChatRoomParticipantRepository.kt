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
            join fetch r.client
            join fetch r.professional
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
            join fetch r.client
            join fetch r.professional
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

    @Query(
        """
            select p
            from ChatRoomParticipant p
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
            select count(p) > 0
            from ChatRoomParticipant p
            where p.room.id = :roomId
              and p.member.username = :username
              and p.visible = true
            """
    )
    fun existsVisibleParticipant(
        @Param("roomId") roomId: Long,
        @Param("username") username: String
    ): Boolean

    @Query(
        """
            select p.room.id
            from ChatRoomParticipant p
            where p.member.username = :username
              and p.visible = true
            """
    )
    fun findVisibleRoomIdsByUsername(
        @Param("username") username: String
    ): List<Long>
}
