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

    /**
     * 반경 계산은 좌표가 채워진 시군구만 대상으로 한다.
     * 전국 시군구가 수백 건 규모라 전량을 메모리에 올려 계산해도 부담이 없다.
     */
    @Query(
        """
        select s
        from Sig s
        where s.latitude is not null
          and s.longitude is not null
        """
    )
    fun findAllWithCoordinate(): List<Sig>
}
