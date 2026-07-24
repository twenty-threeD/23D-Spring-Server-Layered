package spring.springserver.domain.chat.exception

import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.web.bind.annotation.ControllerAdvice
import spring.springserver.global.data.ErrorResponse
import spring.springserver.global.exception.exception.ApplicationException
import java.security.Principal

@ControllerAdvice
class ChatStompControllerAdvice(
    private val messagingTemplate: SimpMessagingTemplate,
) {

    @MessageExceptionHandler
    fun handleApplicationException(
        exception: ApplicationException,
        principal: Principal
    ) {

        messagingTemplate.convertAndSendToUser(
            principal.name,
            "/queue/errors",
            ErrorResponse.of(
                exception.statusCode.getCode(),
                exception.message ?: "Internal error"
            )
        )
    }
}
