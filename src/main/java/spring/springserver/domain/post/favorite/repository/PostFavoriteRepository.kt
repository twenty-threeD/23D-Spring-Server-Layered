package spring.springserver.domain.post.favorite.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.post.entity.Post
import spring.springserver.domain.post.favorite.entity.PostFavorite

interface PostFavoriteRepository: JpaRepository<PostFavorite, Long> {

    fun existsByMemberAndPost(
        member: Member,
        post: Post
    ): Boolean

    fun countByPostId(
        postId: Long
    ): Long

    fun deleteByMemberAndPost(
        member: Member,
        post: Post
    ): Long

    fun deleteAllByPostIn(
        posts: Collection<Post>
    )

    @Query(
        """
        select pf.post
        from PostFavorite pf
        where pf.member = :member
          and pf.post.isDeleted = false
        order by pf.post.updatedAt desc
        """
    )
    fun findFavoritePostsByMember(
        @Param("member") member: Member,
        pageable: Pageable
    ): Page<Post>
}
