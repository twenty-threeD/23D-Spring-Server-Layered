package spring.springserver.domain.call.service.impl

import io.agora.media.AccessToken2
import io.agora.media.RtcTokenBuilder2
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import spring.springserver.domain.call.data.response.AgoraTokenResponse
import spring.springserver.domain.call.exception.CallStatusCode
import spring.springserver.domain.call.service.AgoraTokenService
import spring.springserver.global.config.AgoraProperties
import spring.springserver.global.exception.exception.ApplicationException
import java.time.Instant

@Service
class AgoraTokenServiceImpl(
    private val agoraProperties: AgoraProperties
): AgoraTokenService {

    private val log = LoggerFactory.getLogger(javaClass)

    private val rtcTokenBuilder = RtcTokenBuilder2()

    override fun issueRtcToken(
        channelName: String,
        uid: Int
    ): AgoraTokenResponse {

        val appId = agoraProperties.getTrimmedAppId()
        val appCertificate = agoraProperties.getTrimmedAppCertificate()

        if (appId.isEmpty() || appCertificate.isEmpty()) {

            throw ApplicationException(CallStatusCode.AGORA_NOT_CONFIGURED)
        }

        val expirationSeconds = agoraProperties.tokenExpirationSeconds

        val token = buildToken(
            appId = appId,
            appCertificate = appCertificate,
            channelName = channelName,
            uid = uid,
            expirationSeconds = expirationSeconds
        ) ?: throw ApplicationException(CallStatusCode.AGORA_TOKEN_ISSUE_FAILED)

        return AgoraTokenResponse.of(
            appId = appId,
            token = token,
            expiresAt = Instant.now().plusSeconds(expirationSeconds.toLong())
        )
    }

    /**
     * 기동 직후 설정을 한 번 검증한다.
     *
     * Agora 토큰은 서명이 틀려도 발급 자체는 성공하고, 클라이언트가 채널에 붙는 순간에야
     * 실패한다. 잘못된 키가 배포까지 조용히 흘러가지 않도록 여기서 형식을 확인하고
     * 실제로 한 번 만들어 파싱까지 해 본다.
     */
    @EventListener(ApplicationReadyEvent::class)
    fun verifyConfiguration() {

        val appId = agoraProperties.getTrimmedAppId()
        val appCertificate = agoraProperties.getTrimmedAppCertificate()

        if (appId.isEmpty() || appCertificate.isEmpty()) {

            log.warn("Agora App ID 또는 App Certificate가 없습니다. 통화 API는 비활성 상태로 동작합니다.")

            return
        }

        check(isAgoraKey(appId)) {
            "Agora App ID 형식이 올바르지 않습니다. 32자리 16진수여야 합니다."
        }
        check(isAgoraKey(appCertificate)) {
            "Agora App Certificate 형식이 올바르지 않습니다. 32자리 16진수여야 합니다. " +
                "콘솔에서 App Certificate를 발급받아 활성화했는지 확인하세요."
        }

        val token = buildToken(
            appId = appId,
            appCertificate = appCertificate,
            channelName = VERIFICATION_CHANNEL_NAME,
            uid = VERIFICATION_UID,
            expirationSeconds = VERIFICATION_EXPIRATION_SECONDS
        )

        check(token != null && AccessToken2().parse(token)) {
            "Agora 토큰을 생성하지 못했습니다. App ID와 App Certificate를 확인하세요."
        }

        log.info("Agora 통화 설정 확인 완료. appId={}", maskAppId(appId))
    }

    /**
     * RtcTokenBuilder2는 내부에서 예외를 삼키고 빈 문자열을 돌려준다.
     * 빈 토큰이 클라이언트로 나가지 않도록 여기서 null로 정규화한다.
     */
    private fun buildToken(
        appId: String,
        appCertificate: String,
        channelName: String,
        uid: Int,
        expirationSeconds: Int
    ): String? {

        val token = runCatching {

            rtcTokenBuilder.buildTokenWithUid(
                appId,
                appCertificate,
                channelName,
                uid,
                expirationSeconds,
                expirationSeconds,
                expirationSeconds,
                expirationSeconds,
                expirationSeconds
            )
        }.getOrNull()

        return token?.takeIf { it.isNotBlank() }
    }

    private fun isAgoraKey(
        value: String
    ): Boolean =
        value.length == AGORA_KEY_LENGTH && value.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }

    private fun maskAppId(
        appId: String
    ): String =
        appId.take(APP_ID_PREFIX_LENGTH) + "*".repeat(appId.length - APP_ID_PREFIX_LENGTH)

    companion object {

        private const val AGORA_KEY_LENGTH = 32
        private const val APP_ID_PREFIX_LENGTH = 6
        private const val VERIFICATION_CHANNEL_NAME = "startup-verification"
        private const val VERIFICATION_UID = 1
        private const val VERIFICATION_EXPIRATION_SECONDS = 60
    }
}
