package spring.springserver.domain.like.post.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import spring.springserver.domain.like.post.entity.PostLike
import spring.springserver.domain.member.entity.Member

@Repository
interface PostLikeRepository: JpaRepository<PostLike, Long> {

//    fun countByPostId(postId: Long): Long

    fun findByMember(member: Member): PostLike?
//    fun findByMemberAndPost(member: Member, post: Post): Like?

    fun existsByMember(member: Member): Boolean
//    fun existsByMemberAndPost(member: Member, post: Post): Boolean

//    fun countByPost(post: Post): Long

    fun deleteByMember(member: Member)
//    fun deleteByMemberAndPost(member: Member, post: Post)
}
