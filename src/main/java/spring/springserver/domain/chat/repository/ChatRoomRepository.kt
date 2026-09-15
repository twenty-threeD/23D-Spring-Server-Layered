package spring.springserver.domain.chat.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import spring.springserver.domain.chat.entity.ChatRoom

interface ChatRoomRepository : JpaRepository<ChatRoom, Long> {

    fun findByDirectChatKeyAndPostId(
        directChatKey: String,
        postId: Long
    ): ChatRoom?

    /**
     * 참여자 테이블이 비어 있는 과거 방을 보정하기 위한 조회다.
     * 멤버십 판단에는 쓰지 않는다. 판단은 `ChatRoomParticipantRepository`가 담당한다.
     */
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
