package spring.springserver.domain.community.job.repository

/**
 * 글 단위 집계(댓글 수·좋아요 수) 조회 결과.
 * Object[] 캐스팅 대신 인터페이스 프로젝션을 써서 컬럼 순서와 타입을 컴파일 시점에 묶는다.
 */
interface PostCountProjection {

    fun getPostId(): Long

    fun getCount(): Long
}
