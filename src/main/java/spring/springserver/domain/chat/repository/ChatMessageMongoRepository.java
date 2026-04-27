package spring.springserver.domain.chat.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import spring.springserver.domain.chat.entity.ChatMessageDocument;

import java.util.List;

public interface ChatMessageMongoRepository extends MongoRepository<ChatMessageDocument, String> {

    List<ChatMessageDocument> findAllByRoomIdOrderByCreatedAtAsc(Long roomId);
}
