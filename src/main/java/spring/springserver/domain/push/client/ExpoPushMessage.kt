package spring.springserver.domain.push.client

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Expo Push API 메시지 한 건. 필드명은 Expo 스펙을 그대로 따른다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ExpoPushMessage(
    val to: String,

    val title: String,

    val body: String,

    val data: Map<String, Any?>,

    val sound: String = "default",

    val priority: String = "default",

    /**
     * 안드로이드 알림 채널. 앱이 같은 id로 채널을 만들어 둬야 한다.
     */
    val channelId: String? = null
)
