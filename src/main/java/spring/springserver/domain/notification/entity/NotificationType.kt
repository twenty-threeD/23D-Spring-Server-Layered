package spring.springserver.domain.notification.entity

enum class NotificationType {

    CHAT,
    NOTICE,

    /**
     * 내 지역·카테고리와 맞는 용역 구인/구직 글이 올라왔을 때의 알림.
     */
    JOB_POST
}
