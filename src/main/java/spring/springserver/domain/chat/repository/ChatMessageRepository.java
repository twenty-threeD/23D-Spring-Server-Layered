package spring.springserver.domain.chat.repository;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import spring.springserver.domain.chat.entity.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("""
            select cm
            from ChatMessage cm
            where cm.room.id = :roomId
            order by cm.createdAt asc, cm.id asc
            """)
    List<ChatMessage> findAllByRoomIdOrderByCreatedAtAsc(@Param("roomId") Long roomId);

    @Query("""
            select cm
            from ChatMessage cm
            where cm.room.id = :roomId
              and cm.createdAt > :createdAt
            order by cm.createdAt asc, cm.id asc
            """)
    List<ChatMessage> findAllByRoomIdAndCreatedAtAfterOrderByCreatedAtAsc(@Param("roomId") Long roomId,
                                                                          @Param("createdAt") Instant createdAt);
}
