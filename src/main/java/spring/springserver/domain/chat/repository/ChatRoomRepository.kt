package spring.springserver.domain.chat.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import spring.springserver.domain.chat.entity.ChatRoom

interface ChatRoomRepository : JpaRepository<ChatRoom, Long> {
    
    /**
     * 방은 회원 쌍과 게시글의 조합마다 하나다.
     * 같은 두 사람이라도 게시글이 다르면 다른 방을 쓴다.
     */
    fun findByDirectChatKeyAndPostId(
        directChatKey: String,
        postId: Long
    ): ChatRoom?

    @Query(
        """
            select cr
            from ChatRoom cr
            join fetch cr.client c
            join fetch cr.professional p
            join fetch cr.post
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
}
