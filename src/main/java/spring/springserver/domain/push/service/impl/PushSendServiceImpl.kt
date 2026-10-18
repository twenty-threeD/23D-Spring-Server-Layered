package spring.springserver.domain.push.service.impl

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.push.client.ExpoPushClient
import spring.springserver.domain.push.client.ExpoPushMessage
import spring.springserver.domain.push.repository.PushTokenRepository
import spring.springserver.domain.push.service.PushSendService
import spring.springserver.global.config.ExpoPushProperties

@Service
@Transactional(rollbackFor = [Exception::class])
class PushSendServiceImpl(
    private val pushTokenRepository: PushTokenRepository,
    private val expoPushClient: ExpoPushClient,
    private val expoPushProperties: ExpoPushProperties
): PushSendService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun sendToUsername(
        username: String,
        title: String,
        body: String,
        data: Map<String, Any?>,
        highPriority: Boolean,
        channelId: String?
    ) {

        if (!expoPushProperties.enabled) return

        val tokens = pushTokenRepository.findTokensByUsername(username)

        if (tokens.isEmpty()) return

        val expoPushMessages = tokens.map { token ->

            ExpoPushMessage(
                to = token,
                title = title,
                body = body,
                data = data,
                priority = if (highPriority) "high" else "default",
                channelId = channelId
            )
        }

        val unregisteredTokens = expoPushClient.send(expoPushMessages)

        if (unregisteredTokens.isNotEmpty()) {

            val deletedCount = pushTokenRepository.deleteAllByTokenIn(unregisteredTokens)

            log.info("등록되지 않은 푸시 토큰을 지웠습니다. count = {}", deletedCount)
        }
    }
}
