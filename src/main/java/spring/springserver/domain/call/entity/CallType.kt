package spring.springserver.domain.call.entity

/**
 * 통화를 걸 때의 의도. 수신 화면(벨 UI)과 통화 이력 표기에 쓴다.
 *
 * 연결된 뒤의 실제 송출 상태는 [CallMediaState]가 들고 있으며 통화 중에 자유롭게 바뀐다.
 * 즉 VOICE로 시작한 통화에서도 카메라와 화면 공유를 켤 수 있다.
 */
enum class CallType {

    VOICE,
    VIDEO
}
