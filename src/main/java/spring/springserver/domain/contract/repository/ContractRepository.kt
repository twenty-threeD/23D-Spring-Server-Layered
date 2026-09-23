package spring.springserver.domain.contract.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import spring.springserver.domain.contract.entity.Contract
import spring.springserver.domain.post.entity.Post

interface ContractRepository: JpaRepository<Contract, Long> {

    fun findContractById(
        id: Long
    ): Contract?

    /**
     * 게시글이 보관 기간 만료로 하드 삭제될 때 계약은 거래 자료라 함께 지울 수 없다.
     * fk_contract_post 위반으로 정리 작업 전체가 롤백되지 않도록 게시글 참조만 끊는다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update Contract c
        set c.post = null
        where c.post in :posts
        """
    )
    fun detachFromPosts(
        @Param("posts") posts: Collection<Post>
    ): Int
}
