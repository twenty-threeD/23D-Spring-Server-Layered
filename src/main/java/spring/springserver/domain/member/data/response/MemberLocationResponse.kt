package spring.springserver.domain.member.data.response

data class MemberLocationResponse(
    val sigCd: String,
    val ctprvnCd: String,
    val locationName: String
) {

    companion object {

        fun of(
            sigCd: String,
            ctprvnCd: String,
            locationName: String
        ): MemberLocationResponse {

            return MemberLocationResponse(
                sigCd,
                ctprvnCd,
                locationName
            )
        }
    }
}
