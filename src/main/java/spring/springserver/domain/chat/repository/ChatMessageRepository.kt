package spring.springserver.domain.chat.repository

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import spring.springserver.domain.chat.entity.ChatMessage
import java.time.Instant

interface ChatMessageRepository : JpaRepository<ChatMessage, Long> {

    /**
     * 방의 최신 메시지부터 읽는다. 커서 없이 처음 들어온 요청용이다.
     *
     * `clearBefore`는 방을 나간 시점이다. 나간 적이 없으면 호출부가 `Instant.EPOCH`를 넘겨
     * 널 파라미터 비교(`:param is null`) 없이 항상 참인 조건으로 만든다.
     */
    @Query(
        """
            select m
            from ChatMessage m
            join fetch m.sender
            where m.room.id = :roomId
              and m.createdAt > :clearBefore
            order by m.id desc
            """
    )
    fun findRecentByRoomId(
        @Param("roomId") roomId: Long,
        @Param("clearBefore") clearBefore: Instant,
        pageable: Pageable
    ): List<ChatMessage>

    /**
     * 델타 동기화. 클라이언트가 이미 아는 마지막 메시지 이후만 오래된 -> 최신 순으로 읽는다.
     *
     * 클라이언트 캐시(IndexedDB)는 언제든 사라질 수 있으므로 진실 원본은 늘 이쪽이고,
     * 캐시가 비면 호출부가 `sinceId` 없이 최신 페이지를 받아 스스로 복구한다.
     */
    @Query(
        """
            select m
            from ChatMessage m
            join fetch m.sender
            where m.room.id = :roomId
              and m.id > :sinceId
              and m.createdAt > :clearBefore
            order by m.id asc
            """
    )
    fun findByRoomIdAfter(
        @Param("roomId") roomId: Long,
        @Param("sinceId") sinceId: Long,
        @Param("clearBefore") clearBefore: Instant,
        pageable: Pageable
    ): List<ChatMessage>

    /**
     * 커서(키셋) 페이지네이션. offset을 쓰지 않고 `id < :lastId`로 이전 페이지를 읽는다.
     */
    @Query(
        """
            select m
            from ChatMessage m
            join fetch m.sender
            where m.room.id = :roomId
              and m.id < :lastId
              and m.createdAt > :clearBefore
            order by m.id desc
            """
    )
    fun findByRoomIdBefore(
        @Param("roomId") roomId: Long,
        @Param("lastId") lastId: Long,
        @Param("clearBefore") clearBefore: Instant,
        pageable: Pageable
    ): List<ChatMessage>
}
