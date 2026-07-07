package spring.springserver.domain.location.data.response

import spring.springserver.domain.location.entity.Sig

data class SigunguResponse(
    val sigCd: String,

    val korName: String?,

    val engName: String?
) {

    companion object {

        fun of(
            sig: Sig
        ): SigunguResponse {

            return SigunguResponse(
                sig.getSigCd(),
                sig.sigKorNm,
                sig.sigEngNm
            )
        }
    }
}