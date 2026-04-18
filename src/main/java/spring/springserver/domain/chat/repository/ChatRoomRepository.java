package spring.springserver.domain.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import spring.springserver.domain.chat.entity.ChatRoom;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
}
