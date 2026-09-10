package spring.springserver.domain.call.data.response

import spring.springserver.domain.call.entity.Call
import spring.springserver.domain.member.entity.Member

/**
 * 통화 참여자 한 명의 Agora uid와 현재 송출 상태.
 *
 * 클라이언트는 원격 스트림을 uid로 구분하므로 두 uid를 모두 내려준다.
 * `screenSharing`이 true인 참여자의 `screenUid` 스트림이 화면 공유 영상이다.
 */
data class CallParticipantResponse(
    val username: String,
    val name: String,
    val uid: Int,
    val screenUid: Int,
    val audioEnabled: Boolean,
    val videoEnabled: Boolean,
    val screenSharing: Boolean
) {

    companion object {

        fun of(
            call: Call,
            member: Member
        ): CallParticipantResponse {

            val memberId = member.getId()
            val media = call.mediaOf(memberId)

            return CallParticipantResponse(
                username = member.username,
                name = member.name,
                uid = call.cameraUidOf(memberId),
                screenUid = call.screenUidOf(memberId),
                audioEnabled = media.audioEnabled,
                videoEnabled = media.videoEnabled,
                screenSharing = call.isScreenSharing(memberId)
            )
        }
    }
}
