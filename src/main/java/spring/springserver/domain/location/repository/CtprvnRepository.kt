package spring.springserver.domain.location.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import spring.springserver.domain.location.entity.Ctprvn

interface CtprvnRepository: JpaRepository<Ctprvn, String> {

    @Query("select c from Ctprvn c order by c.ctprvnCd asc")
    fun findAllOrderByCtprvnCd(): List<Ctprvn>
}