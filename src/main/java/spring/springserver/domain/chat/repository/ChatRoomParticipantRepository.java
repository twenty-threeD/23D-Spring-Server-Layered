package spring.springserver.domain.chat.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import spring.springserver.domain.chat.entity.ChatRoomParticipant;

public interface ChatRoomParticipantRepository extends JpaRepository<ChatRoomParticipant, Long> {

    @Query("""
            select p
            from ChatRoomParticipant p
            join fetch p.room r
            join fetch r.client
            join fetch r.professional
            where p.member.username = :username
              and p.visible = true
            order by
                case when r.lastMessageAt is null then 1 else 0 end,
                r.lastMessageAt desc,
                r.id desc
            """)
    List<ChatRoomParticipant> findVisibleParticipantsByUsername(@Param("username") String username);

    @Query("""
            select p
            from ChatRoomParticipant p
            join fetch p.room r
            join fetch r.client
            join fetch r.professional
            where r.id = :roomId
              and p.member.username = :username
            """)
    Optional<ChatRoomParticipant> findByRoomIdAndMemberUsername(@Param("roomId") Long roomId,
                                                                @Param("username") String username);

    @Query("""
            select p
            from ChatRoomParticipant p
            join fetch p.member
            where p.room.id = :roomId
            """)
    List<ChatRoomParticipant> findAllByRoomId(@Param("roomId") Long roomId);

    @Query("""
            select p
            from ChatRoomParticipant p
            where p.room.id = :roomId
              and p.member.id = :memberId
            """)
    Optional<ChatRoomParticipant> findByRoomIdAndMemberId(@Param("roomId") Long roomId,
                                                          @Param("memberId") Long memberId);

    @Query("""
            select count(p) > 0
            from ChatRoomParticipant p
            where p.room.id = :roomId
              and p.member.username = :username
              and p.visible = true
            """)
    boolean existsVisibleParticipant(@Param("roomId") Long roomId,
                                     @Param("username") String username);
}
