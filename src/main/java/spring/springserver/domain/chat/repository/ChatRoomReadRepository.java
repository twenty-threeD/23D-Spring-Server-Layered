package spring.springserver.domain.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import spring.springserver.domain.chat.entity.ChatRoomRead;

public interface ChatRoomReadRepository extends JpaRepository<ChatRoomRead, Long> {
}
