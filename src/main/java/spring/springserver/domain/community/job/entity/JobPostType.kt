package spring.springserver.domain.community.job.entity

/**
 * 구인/구직 게시판의 글 종류.
 *
 * 구인/구직 글은 community_job_post 테이블에만 담기므로
 * "일반 커뮤니티 글"을 가리키는 값은 여기에 두지 않는다.
 * 일반 커뮤니티 글은 community_post 테이블이 따로 갖고 있다.
 */
enum class JobPostType(private val label: String) {

    HIRING("구인"),
    SEEKING("구직");

    fun getLabel(): String = label
}
