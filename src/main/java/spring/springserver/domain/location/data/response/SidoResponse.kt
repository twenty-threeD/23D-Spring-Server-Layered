package spring.springserver.domain.location.data.response

import spring.springserver.domain.location.entity.Ctprvn

data class SidoResponse(
    val ctprvnCd: String,

    val korName: String?,

    val engName: String?
) {

    companion object {

        fun of(
            ctprvn: Ctprvn
        ): SidoResponse {

            return SidoResponse(
                ctprvn.ctprvnCd,
                ctprvn.ctpKorNm,
                ctprvn.ctpEngNm
            )
        }
    }
}