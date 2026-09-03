package spring.springserver.domain.blockchain.exception

/**
 * 같은 order_id 로 이미 체인에 기록된 경우 발생한다. 재시도 관점에서는 성공으로 취급한다.
 */
class BlockchainAlreadyRecordedException(
    message: String
): RuntimeException(message)