package spring.springserver.domain.chat.data.response

import spring.springserver.domain.call.entity.CallStatus
import spring.springserver.domain.call.entity.CallType

/**
 * 채팅방에 남는 통화 기록 한 건. 통화가 끝난 뒤 서버가 발신자 이름으로 남긴다.
 *
 * @property status 끝난 결과. ENDED(통화함), MISSED(부재중), REJECTED(거절), CANCELED(발신 취소)
 * @property durationSeconds 실제로 통화한 시간. 연결되지 않은 통화는 null이다.
 */
data class ChatCallLogResponse(
    val callId: Long?,
    val callType: CallType,
    val status: CallStatus,
    val durationSeconds: Long?,
    val callerUsername: String
) {

    /**
     * 채팅 목록 미리보기와 메시지 본문에 쓰는 문구.
     */
    fun toPreview(): String {

        val label = if (callType == CallType.VIDEO) "영상 통화" else "음성 통화"

        return when (status) {

            CallStatus.ENDED -> "$label ${formatDuration(durationSeconds ?: 0)}"
            CallStatus.MISSED -> "부재중 $label"
            CallStatus.REJECTED -> "$label 거절"
            CallStatus.CANCELED -> "$label 취소"
            else -> label
        }
    }

    private fun formatDuration(
        seconds: Long
    ): String {

        val hours = seconds / 3600
        val minutes = seconds % 3600 / 60
        val remainSeconds = seconds % 60

        return when {

            hours > 0 -> "${hours}시간 ${minutes}분"
            minutes > 0 -> "${minutes}분 ${remainSeconds}초"
            else -> "${remainSeconds}초"
        }
    }

    companion object {

        fun of(
            callId: Long?,
            callType: CallType,
            status: CallStatus,
            durationSeconds: Long?,
            callerUsername: String
        ): ChatCallLogResponse {

            return ChatCallLogResponse(
                callId = callId,
                callType = callType,
                status = status,
                durationSeconds = durationSeconds,
                callerUsername = callerUsername
            )
        }
    }
}
