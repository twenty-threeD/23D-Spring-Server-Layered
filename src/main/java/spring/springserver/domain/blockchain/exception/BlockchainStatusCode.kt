package spring.springserver.domain.blockchain.exception

import org.springframework.http.HttpStatus
import spring.springserver.global.exception.status_code.StatusCode

enum class BlockchainStatusCode(
    private val code: String,
    private val message: String,
    private val httpStatus: HttpStatus
) : StatusCode {

    BLOCKCHAIN_NODE_UNAVAILABLE("BLOCKCHAIN_NODE_UNAVAILABLE", "노드에 연결할 수 없습니다.", HttpStatus.BAD_GATEWAY);

    override fun getCode(): String = code
    override fun getMessage(): String = message
    override fun getHttpStatus(): HttpStatus = httpStatus
}
