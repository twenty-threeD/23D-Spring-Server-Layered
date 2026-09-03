package spring.springserver.domain.location.service

import spring.springserver.domain.location.data.response.SidoResponse
import spring.springserver.domain.location.data.response.SigunguResponse
import spring.springserver.domain.location.entity.Sig

interface LocationService {

    fun getSidoList(): List<SidoResponse>

    fun getSigunguList(
        ctprvnCd: String
    ): List<SigunguResponse>

    fun getSig(
        sigCd: String
    ): Sig

    fun getFullName(
        sig: Sig
    ): String

    /**
     * 기준 시군구 중심점에서 반경 안에 들어오는 시군구 코드를 돌려준다.
     * 기준 시군구나 비교 대상에 좌표가 없으면 거리를 알 수 없으므로,
     * 최소한 기준 시군구 자신만 담아 돌려준다.
     */
    fun findNearbySigCds(
        sigCd: String,
        radiusKm: Double
    ): List<String>
}
