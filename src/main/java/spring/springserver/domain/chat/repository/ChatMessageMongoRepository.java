package spring.springserver.domain.chat.repository;

import java.time.Instant;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import spring.springserver.domain.chat.entity.ChatMessageDocument;

public interface ChatMessageMongoRepository extends MongoRepository<ChatMessageDocument, String> {

    List<ChatMessageDocument> findAllByRoomIdOrderByCreatedAtAsc(Long roomId);

    List<ChatMessageDocument> findAllByRoomIdAndCreatedAtAfterOrderByCreatedAtAsc(Long roomId,
                                                                                  Instant createdAt);
}
