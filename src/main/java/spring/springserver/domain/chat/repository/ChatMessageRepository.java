package spring.springserver.domain.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import spring.springserver.domain.chat.entity.ChatMessage;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("""
            select cm
            from ChatMessage cm
            join fetch cm.member
            where cm.chatRoom.id = :roomId
            order by cm.createdAt asc, cm.id asc
            """)
    List<ChatMessage> findAllByRoomIdOrderByCreatedAtAsc(@Param("roomId") Long roomId);
}
