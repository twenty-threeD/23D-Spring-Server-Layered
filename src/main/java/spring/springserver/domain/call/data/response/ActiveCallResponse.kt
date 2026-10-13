package spring.springserver.domain.call.data.response

/**
 * 내가 참여 중인 진행 중 통화.
 *
 * @property connection 채널에 다시 접속할 정보. 수신자가 아직 수락하지 않은 통화면 null이다.
 *                      수락 전에 토큰을 주면 발신자가 이미 송출 중인 음성·영상을 들을 수 있기 때문이다.
 */
data class ActiveCallResponse(
    val call: CallResponse,
    val connection: CallConnectionResponse?
) {

    companion object {

        fun of(
            call: CallResponse,
            connection: CallConnectionResponse?
        ): ActiveCallResponse =
            ActiveCallResponse(
                call = call,
                connection = connection
            )
    }
}
