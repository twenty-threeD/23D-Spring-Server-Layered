package spring.springserver.domain.call.data.response

enum class CallSignalType {

    INVITED,
    ACCEPTED,
    REJECTED,
    CANCELED,
    MISSED,
    ENDED,

    /**
     * 마이크/카메라 송출 상태가 바뀌었다. 음성 통화 중 카메라를 켜는 경우도 여기로 온다.
     */
    MEDIA_CHANGED,

    SCREEN_SHARE_STARTED,
    SCREEN_SHARE_STOPPED
}
