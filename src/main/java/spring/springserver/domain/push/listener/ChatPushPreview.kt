package spring.springserver.domain.push.listener

import spring.springserver.domain.chat.data.response.ChatMessageResponse
import spring.springserver.domain.chat.data.response.ChatMessageType

/**
 * 채팅 메시지를 푸시 본문으로 바꾼다. 웹 `chatPreview.ts`의 previewOf와 같은 규칙을 따른다.
 * 대괄호 접두사 특수 메시지는 뒤에 JSON이 붙어 오므로 원문 대신 짧은 문구를 쓴다.
 */
object ChatPushPreview {

    private const val MAX_BODY_LENGTH = 100

    /**
     * 긴 접두사를 먼저 둔다. "[거래 완료]"가 "[거래 완료 요청]"을 먼저 잡으면 안 된다.
     */
    private val SPECIAL_PREFIXES = listOf(
        "[계약서 제안]" to "계약서를 보냈어요",
        "[계약서 체결 완료]" to "계약이 체결됐어요",
        "[결제 완료]" to "결제가 완료됐어요",
        "[거래 완료 요청]" to "거래 완료를 기다리고 있어요",
        "[거래 완료]" to "거래가 완료됐어요",
        "[거래 취소]" to "거래가 취소됐어요"
    )

    /**
     * 푸시를 보내지 않을 메시지면 null. 통화 기록은 통화 푸시가 따로 나간다.
     */
    fun of(
        chatMessageResponse: ChatMessageResponse
    ): String? {

        if (chatMessageResponse.type == ChatMessageType.CALL) return null

        if (chatMessageResponse.type == ChatMessageType.PAYMENT) return "결제가 완료됐어요"

        val text = chatMessageResponse.message.trim()

        if (text.isEmpty()) {

            return if (chatMessageResponse.attachedFileUrls.isNotEmpty()) "사진을 보냈습니다." else null
        }

        SPECIAL_PREFIXES.firstOrNull { (prefix, _) -> text.startsWith(prefix) }
            ?.let { (_, preview) -> return preview }

        if (text.startsWith("{")) return "새 메시지가 왔어요"

        val singleLine = text.replace(Regex("\\s+"), " ")

        return if (singleLine.length > MAX_BODY_LENGTH) singleLine.take(MAX_BODY_LENGTH) + "…" else singleLine
    }
}
