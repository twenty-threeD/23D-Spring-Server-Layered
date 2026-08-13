package spring.springserver.domain.post.category.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import spring.springserver.domain.post.category.entity.PostCategory

@Repository
interface PostCategoryRepository: JpaRepository<PostCategory, Long> {

    fun findAllByOrderByNameAsc(): List<PostCategory>

    @Query(
        """
        select c.id
        from PostCategory c
        where c.parent.id in :parentIds
        """
    )
    fun findIdsByParentIdIn(
        @Param("parentIds") parentIds: Collection<Long>
    ): List<Long>
}
