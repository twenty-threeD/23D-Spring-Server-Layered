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
}