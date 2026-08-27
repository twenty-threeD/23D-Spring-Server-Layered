package spring.springserver.domain.community.post.entity

/**
 * 게시글이 어느 게시판에 속하는지를 나타낸다.
 * 구인/구직 전용 구분이 아니라 community_post 테이블 전체의 게시판 구분이다.
 *
 * 구인/구직 글이 기존 커뮤니티와 같은 테이블을 쓰기로 했기 때문에,
 * "일반 커뮤니티 글"을 가리키는 값이 반드시 하나 있어야 한다. 그게 GENERAL이다.
 * GENERAL이 없으면 아래가 성립하지 않는다.
 *
 * - 기존에 쌓인 글의 기본값이 없어진다. (CommunityPost.postType 의 컬럼 기본값 'GENERAL')
 * - 일반 커뮤니티 목록·검색에서 구인/구직 글을 걸러낼 수 없다.
 *   (CommunityPostServiceImpl 의 getPosts / searchPosts)
 * - 구인/구직 글인지 판정할 기준이 없어진다. (isJobPost)
 *
 * 게시판이 늘어나면 여기에 항목을 추가한다.
 * 반대로 구인/구직을 별도 테이블로 분리하게 되면 GENERAL은 그때 사라진다.
 */
enum class CommunityPostType(private val label: String) {

    GENERAL("일반"),
    HIRING("구인"),
    SEEKING("구직");

    fun getLabel(): String = label

    /**
     * 구인/구직 글은 카테고리와 지역이 반드시 있어야 한다.
     */
    fun isJobPost(): Boolean = this != GENERAL

    companion object {

        fun jobPostTypes(): List<CommunityPostType> = entries.filter { it.isJobPost() }
    }
}
