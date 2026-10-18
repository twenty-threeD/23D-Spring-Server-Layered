package spring.springserver.domain.push.service

interface PushSendService {

    /**
     * 회원의 모든 기기로 푸시를 보낸다. 실패해도 예외를 올리지 않는다.
     */
    fun sendToUsername(
        username: String,
        title: String,
        body: String,
        data: Map<String, Any?>,
        highPriority: Boolean = false,
        channelId: String? = null
    )
}
