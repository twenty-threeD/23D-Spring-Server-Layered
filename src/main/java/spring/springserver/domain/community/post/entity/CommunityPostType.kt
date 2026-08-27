package spring.springserver.domain.community.post.entity

/**
 * 게시글이 어느 게시판에 속하는지를 나타낸다.
 * GENERAL은 기존 커뮤니티 글이고, HIRING·SEEKING은 용역 구인/구직 커뮤니티 글이다.
 * 게시판이 늘어나면 여기에 항목을 추가한다.
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
