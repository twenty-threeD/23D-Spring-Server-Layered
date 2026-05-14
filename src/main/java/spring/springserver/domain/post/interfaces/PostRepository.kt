package spring.springserver.domain.post.interfaces

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import spring.springserver.domain.post.entity.Post

@Repository
interface PostRepository : JpaRepository<Post, Long> {

    fun findPostById(id: Long): Post?
}
