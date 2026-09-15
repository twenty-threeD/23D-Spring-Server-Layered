package spring.springserver.domain.chat.repository

/**
 * 방-회원 참여 조합 조회 결과.
 * Object[] 캐스팅 대신 인터페이스 프로젝션을 써서 컬럼 순서와 타입을 컴파일 시점에 묶는다.
 */
interface RoomMemberIdProjection {

    fun getRoomId(): Long

    fun getMemberId(): Long
}
