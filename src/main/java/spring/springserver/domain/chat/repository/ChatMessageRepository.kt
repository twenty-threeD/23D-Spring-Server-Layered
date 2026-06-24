package spring.springserver.domain.chat.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import spring.springserver.domain.chat.entity.ChatMessage
import java.time.Instant

interface ChatMessageRepository : JpaRepository<ChatMessage, Long> {

    @Query(
        """
            select cm
            from ChatMessage cm
            where cm.room.id = :roomId
            order by cm.createdAt asc, cm.id asc
            """
    )
    fun findAllByRoomIdOrderByCreatedAtAsc(
        @Param("roomId") roomId: Long
    ): List<ChatMessage>

    @Query(
        """
            select cm
            from ChatMessage cm
            where cm.room.id = :roomId
              and cm.createdAt > :createdAt
            order by cm.createdAt asc, cm.id asc
            """
    )
    fun findAllByRoomIdAndCreatedAtAfterOrderByCreatedAtAsc(
        @Param("roomId") roomId: Long,
        @Param("createdAt") createdAt: Instant
    ): List<ChatMessage>
}
