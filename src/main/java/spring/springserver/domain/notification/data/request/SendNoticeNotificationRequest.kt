package spring.springserver.domain.notification.data.request

import jakarta.validation.constraints.NotBlank

data class SendNoticeNotificationRequest(
    @field:NotBlank(message = "받는 사람은 필수입니다.")
    val receiverUsername: String,

    @field:NotBlank(message = "알림 내용은 필수입니다.")
    val message: String
)