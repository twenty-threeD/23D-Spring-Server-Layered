package spring.springserver.domain.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import spring.springserver.domain.chat.entity.ChatRoom;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("""
            select cr
            from ChatRoom cr
            join fetch cr.client c
            join fetch cr.professional p
            where (c.username = :username1 and p.username = :username2)
               or (c.username = :username2 and p.username = :username1)
            """)
    Optional<ChatRoom> findDirectRoomByUsernames(@Param("username1") String username1,
                                                 @Param("username2") String username2);

    Optional<ChatRoom> findByDirectChatKey(String directChatKey);

    @Query("""
            select cr
            from ChatRoom cr
            join fetch cr.client c
            join fetch cr.professional p
            where c.username = :username
               or p.username = :username
            order by
                case when cr.lastMessageAt is null then 1 else 0 end,
                cr.lastMessageAt desc,
                cr.id desc
            """)
    List<ChatRoom> findAllByParticipantUsername(@Param("username") String username);

    @Query("""
            select cr
            from ChatRoom cr
            join fetch cr.client
            join fetch cr.professional
            where cr.id = :roomId
            """)
    Optional<ChatRoom> findByIdWithParticipants(@Param("roomId") Long roomId);
}
