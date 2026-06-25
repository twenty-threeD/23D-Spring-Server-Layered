package spring.springserver.global.config.websocket

import java.security.Principal

data class StompPrincipal(
    val username: String,
    val role: String?
) : Principal {

    override fun getName(): String = username
}
