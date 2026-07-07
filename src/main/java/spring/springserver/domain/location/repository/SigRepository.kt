package spring.springserver.domain.location.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import spring.springserver.domain.location.entity.Sig

interface SigRepository: JpaRepository<Sig, String> {

    @Query(
        """
        select s
        from Sig s
        where substring(s.sigCd, 1, 2) = :ctprvnCd
        order by s.sigCd asc
        """
    )
    fun findAllByCtprvnCd(@Param("ctprvnCd") ctprvnCd: String): List<Sig>
}