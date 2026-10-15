package spring.springserver.domain.call.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

/**
 * 통화 참여자 한 명의 송출 상태.
 *
 * Agora 토큰은 음성/영상 권한을 모두 담고 나가므로 실제 켜고 끄는 것은 클라이언트가 한다.
 * 서버는 그 상태를 받아 상대에게 전달하는 단일 진실 공급원 역할만 한다.
 * 이렇게 해야 음성으로 시작한 통화에서 토큰 재발급 없이 바로 카메라를 켤 수 있다.
 */
@Embeddable
class CallMediaState(
    @Column(name = "audio_enabled", nullable = false)
    var audioEnabled: Boolean = true,

    @Column(name = "video_enabled", nullable = false)
    var videoEnabled: Boolean = false
) {

    fun update(
        audioEnabled: Boolean,
        videoEnabled: Boolean
    ) {

        this.audioEnabled = audioEnabled
        this.videoEnabled = videoEnabled
    }

    companion object {

        fun of(
            callType: CallType
        ): CallMediaState =
            CallMediaState(
                audioEnabled = true,
                videoEnabled = callType == CallType.VIDEO
            )
    }
}
