package spring.springserver.domain.blockchain.exception

/**
 * 커밋을 기다리는 동안 결과를 확정하지 못했을 때 던진다.
 * 실패가 확정된 것이 아니라 아직 모르는 상태이므로, 뒤늦게 커밋될 수 있는 트랜잭션 해시를 함께 넘긴다.
 */
class BlockchainCommitTimeoutException(
    message: String,
    val txHash: String? = null
): RuntimeException(message)
