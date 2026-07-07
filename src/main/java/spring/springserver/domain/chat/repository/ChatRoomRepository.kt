package spring.springserver.domain.chat.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import spring.springserver.domain.chat.entity.ChatRoom

interface ChatRoomRepository : JpaRepository<ChatRoom, Long> {

    @Query(
        """
            select cr
            from ChatRoom cr
            join fetch cr.client c
            join fetch cr.professional p
            where (c.username = :username1 and p.username = :username2)
               or (c.username = :username2 and p.username = :username1)
            """
    )
    fun findDirectRoomByUsernames(
        @Param("username1") username1: String,
        @Param("username2") username2: String
    ): ChatRoom?

    fun findByDirectChatKey(
        directChatKey: String
    ): ChatRoom?

    @Query(
        """
            select cr
            from ChatRoom cr
            join fetch cr.client c
            join fetch cr.professional p
            where c.username = :username
               or p.username = :username
            order by
                case when cr.lastMessageAt is null then 1 else 0 end,
                cr.lastMessageAt desc,
                cr.id desc
            """
    )
    fun findAllByParticipantUsername(
        @Param("username") username: String
    ): List<ChatRoom>

    @Query(
        """
            select cr
            from ChatRoom cr
            join fetch cr.client
            join fetch cr.professional
            where cr.id = :roomId
            """
    )
    fun findByIdWithParticipants(
        @Param("roomId") roomId: Long
    ): ChatRoom?

    @Query(
        """
            select count(cr) > 0
            from ChatRoom cr
            join cr.client c
            join cr.professional p
            where cr.id = :roomId
              and (c.username = :username or p.username = :username)
            """
    )
    fun existsAuthorizedRoom(
        @Param("roomId") roomId: Long,
        @Param("username") username: String
    ): Boolean
}
