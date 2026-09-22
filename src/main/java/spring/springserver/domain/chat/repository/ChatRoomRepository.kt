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

    /**
     * 참여자 row가 아직 없는 과거 방의 개수다. 0이면 보정할 것이 없다.
     *
     * 방 집합(client/professional 컬럼)과 참여자 테이블은 서로 독립적이라
     * 어느 한쪽이 비었는지로는 보정 필요 여부를 알 수 없다. 참여자 row가 일부만 있는
     * 회원도 있으므로 "빠진 방이 있는가"를 직접 물어야 한다.
     */
    @Query(
        """
            select count(cr)
            from ChatRoom cr
            where (cr.client.username = :username or cr.professional.username = :username)
              and not exists (
                select 1
                from ChatRoomParticipant p
                where p.room = cr
                  and p.member.username = :username
              )
            """
    )
    fun countRoomsMissingParticipantRow(
        @Param("username") username: String
    ): Long

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
