package spring.springserver.domain.blockchain.service

import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.crypto.digests.RIPEMD160Digest
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECPrivateKeySpec
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import spring.springserver.domain.blockchain.exception.BlockchainAlreadyRecordedException
import spring.springserver.domain.blockchain.exception.BlockchainCommitTimeoutException
import spring.springserver.domain.blockchain.exception.BlockchainSequenceMismatchException
import spring.springserver.global.config.blockchain.CosmosProperties
import java.math.BigInteger
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.util.Base64
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Service
class BlockchainService(
    private val cosmosProperties: CosmosProperties
) {

    private val log = LoggerFactory.getLogger(BlockchainService::class.java)

    private val restTemplate = RestTemplate()

    private val sequenceLock = ReentrantLock()

    @Volatile
    private var cachedAccountNumber: Long? = null

    @Volatile
    private var cachedSequence: Long? = null

    private val submitterPubKeyBytes: ByteArray by lazy {

        ECNamedCurveTable.getParameterSpec("secp256k1").g
            .multiply(BigInteger(cosmosProperties.submitterPrivateKey, 16))
            .normalize()
            .getEncoded(true)
    }

    val submitterAddress: String by lazy {

        deriveCosmosAddress(submitterPubKeyBytes)
    }

    /**
     * itda.payment.v1.MsgRecordPayment 를 브로드캐스트하고 체인에 포함된 트랜잭션 해시를 돌려준다.
     * authority 는 제네시스에 설정된 주소여야 하며, submitter 개인키가 그 주소를 가리켜야 한다.
     */
    fun recordPayment(
        buyerAddress: String,
        orderId: String,
        amount: Long,
        paidAt: String,
        contractUrl: String,
        paymentHash: String,
        buyerSignature: String
    ): String {

        val msgBytes = buildProto {
            string(1, submitterAddress)
            string(2, orderId)
            string(3, buyerAddress)
            uint64(4, amount)
            string(5, paidAt)
            string(6, contractUrl)
            string(7, paymentHash)
            string(8, buyerSignature)
        }

        val anyBytes = buildProto {
            string(1, "/itda.payment.v1.MsgRecordPayment")
            bytes(2, msgBytes)
        }

        val txBodyBytes = buildProto {
            embedded(1, anyBytes)
            string(2, "")
        }

        val txHash = broadcastInOrder(txBodyBytes)

        awaitCommit(
            txHash,
            orderId
        )

        return txHash
    }

    /**
     * submitter 계정의 시퀀스는 하나뿐이므로 조립·서명·브로드캐스트를 직렬화한다.
     * 커밋 대기는 락 밖에서 해야 처리량이 블록 생성 시간에 묶이지 않는다.
     */
    private fun broadcastInOrder(
        txBodyBytes: ByteArray
    ): String {

        return sequenceLock.withLock {

            try {

                signAndBroadcast(
                    txBodyBytes,
                    nextSequence()
                )
            } catch (exception: BlockchainSequenceMismatchException) {

                log.warn("시퀀스가 어긋나 노드에서 재동기화 후 재시도합니다.", exception)

                resetSequenceCache()

                signAndBroadcast(
                    txBodyBytes,
                    nextSequence()
                )
            }
        }
    }

    private fun signAndBroadcast(
        txBodyBytes: ByteArray,
        sequence: Long
    ): String {

        val pubKeyAnyBytes = buildProto {
            string(1, "/cosmos.crypto.secp256k1.PubKey")
            bytes(2, buildProto { bytes(1, submitterPubKeyBytes) })
        }

        val signerInfoBytes = buildProto {
            embedded(1, pubKeyAnyBytes)
            embedded(2, buildProto { embedded(1, buildProto { uint64(1, 1L) }) })
            uint64(3, sequence)
        }

        val authInfoBytes = buildProto {
            embedded(1, signerInfoBytes)
            embedded(2, buildProto { uint64(2, 200000L) })
        }

        val signDocBytes = buildProto {
            bytes(1, txBodyBytes)
            bytes(2, authInfoBytes)
            string(3, cosmosProperties.chainId)
            uint64(4, accountNumber())
            // Sequence intentionally omitted: this fork's GetSignBytes excludes it
        }

        val signature = signBytes(signDocBytes)

        val txRawBytes = buildProto {
            bytes(1, txBodyBytes)
            bytes(2, authInfoBytes)
            bytes(3, signature)
        }

        val txHash = broadcast(Base64.getEncoder().encodeToString(txRawBytes))

        cachedSequence = sequence + 1

        return txHash
    }

    /**
     * account_number 는 계정이 만들어진 뒤 바뀌지 않으므로 한 번만 조회한다.
     */
    private fun accountNumber(): Long {

        cachedAccountNumber?.let { return it }

        syncFromNode()

        return cachedAccountNumber
            ?: error("account number를 조회하지 못했습니다.")
    }

    private fun nextSequence(): Long {

        cachedSequence?.let { return it }

        syncFromNode()

        return cachedSequence
            ?: error("sequence를 조회하지 못했습니다.")
    }

    private fun syncFromNode() {

        val (accountNumber, sequence) = getAccountInfo(submitterAddress)

        cachedAccountNumber = accountNumber
        cachedSequence = sequence
    }

    private fun resetSequenceCache() {

        cachedSequence = null
    }

    private fun getAccountInfo(address: String): Pair<Long, Long> {

        val url = "${cosmosProperties.nodeUrl}/cosmos/auth/v1beta1/accounts/$address"
        val response = restTemplate.getForObject(url, Map::class.java)
        val account = response?.get("account") as? Map<*, *>
            ?: error("account not found")
        val accountNumber = (account["account_number"] as? String)?.toLong() ?: 0L
        val sequence = (account["sequence"] as? String)?.toLong() ?: 0L

        return Pair(accountNumber, sequence)
    }

    private fun broadcast(txBase64: String): String {

        val url = "${cosmosProperties.nodeUrl}/cosmos/tx/v1beta1/txs"
        val body = mapOf(
            "tx_bytes" to txBase64,
            "mode" to "BROADCAST_MODE_SYNC"
        )
        val headers = HttpHeaders().also { it.contentType = MediaType.APPLICATION_JSON }
        val response = restTemplate.postForObject(
            url,
            HttpEntity(body, headers),
            Map::class.java
        )
        val txResponse = response?.get("tx_response") as? Map<*, *>
            ?: error("broadcast response is empty")

        verifyResult(txResponse)

        return txResponse["txhash"]?.toString()
            ?: error("txhash not found")
    }

    /**
     * BROADCAST_MODE_SYNC 는 CheckTx 결과만 돌려주므로, 메시지 실행 성공 여부는 조회로 확인해야 한다.
     * 폴링이 시간 안에 끝나지 않으면 원장 조회로 기록 여부를 확정한다.
     * 기록됐는데 실패로 판정하면 결제만 취소되고 체인에는 기록이 남아 되돌릴 수 없기 때문이다.
     */
    private fun awaitCommit(
        txHash: String,
        orderId: String
    ) {

        val url = "${cosmosProperties.nodeUrl}/cosmos/tx/v1beta1/txs/$txHash"

        repeat(COMMIT_POLL_MAX_ATTEMPTS) {

            val txResponse = runCatching {

                (restTemplate.getForObject(url, Map::class.java))?.get("tx_response") as? Map<*, *>
            }.getOrNull()

            if (txResponse != null) {

                verifyResult(txResponse)

                return
            }

            Thread.sleep(COMMIT_POLL_INTERVAL_MILLIS)
        }

        if (isRecordedOnChain(orderId)) {

            log.warn("커밋 폴링은 시간을 초과했지만 원장에 기록이 확인되어 성공으로 처리합니다. orderId={}, txhash={}", orderId, txHash)

            return
        }

        throw BlockchainCommitTimeoutException("tx not committed within timeout (txhash=$txHash)")
    }

    /**
     * 주문이 원장에 실제로 기록됐는지 확인한다. 조회 자체가 실패하면 알 수 없으므로 false 로 본다.
     */
    private fun isRecordedOnChain(
        orderId: String
    ): Boolean {

        val url = "${cosmosProperties.nodeUrl}/itda/payment/v1/payments/$orderId"

        return runCatching {

            (restTemplate.getForObject(url, Map::class.java))?.get("record") != null
        }.getOrElse {

            log.warn("원장 기록 조회에 실패했습니다. orderId={}", orderId, it)

            false
        }
    }

    private fun verifyResult(txResponse: Map<*, *>) {

        val code = txResponse["code"]?.toString()?.toInt() ?: -1

        if (code == 0) return

        val codespace = txResponse["codespace"]?.toString()
        val rawLog = txResponse["raw_log"]

        if (codespace == PAYMENT_CODESPACE && code == DUPLICATE_ORDER_ID_CODE) {

            throw BlockchainAlreadyRecordedException("이미 기록된 주문입니다: $rawLog")
        }

        if (codespace == SDK_CODESPACE && code == WRONG_SEQUENCE_CODE) {

            throw BlockchainSequenceMismatchException("시퀀스가 일치하지 않습니다: $rawLog")
        }

        error("chain rejected tx (codespace=$codespace, code=$code): $rawLog")
    }

    private fun signBytes(data: ByteArray): ByteArray {

        val privInt = BigInteger(cosmosProperties.submitterPrivateKey, 16)
        val privateKey = KeyFactory.getInstance(
            "EC",
            BouncyCastleProvider()
        ).generatePrivate(
            ECPrivateKeySpec(
                privInt,
                ECNamedCurveTable.getParameterSpec("secp256k1")
            )
        )
        val signer = Signature.getInstance(
            "SHA256withECDSA",
            BouncyCastleProvider()
        )

        signer.initSign(privateKey)
        signer.update(data)

        val derSig = signer.sign()
        val seq = ASN1Sequence.getInstance(derSig)
        var r = (seq.getObjectAt(0) as ASN1Integer).positiveValue
        var s = (seq.getObjectAt(1) as ASN1Integer).positiveValue
        val n = BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16)

        if (s > n.shiftRight(1)) s = n.subtract(s)

        val sig64 = ByteArray(64)
        val rBytes = r.toByteArray().let { if (it.size == 33) it.copyOfRange(1, 33) else it }
        val sBytes = s.toByteArray().let { if (it.size == 33) it.copyOfRange(1, 33) else it }

        rBytes.copyInto(sig64, 32 - rBytes.size)
        sBytes.copyInto(sig64, 64 - sBytes.size)

        return sig64
    }

    private fun deriveCosmosAddress(pubKeyBytes: ByteArray): String {

        val sha256 = MessageDigest.getInstance("SHA-256").digest(pubKeyBytes)
        val ripemd160 = ByteArray(20)

        RIPEMD160Digest().apply {
            update(sha256, 0, sha256.size)
            doFinal(ripemd160, 0)
        }

        val converted = convertBits(ripemd160, 8, 5)

        return "cosmos1" + converted.joinToString("") { BECH32_CHARSET[it].toString() } +
                bech32Checksum("cosmos", converted)
    }

    private fun buildProto(block: ProtoBuilder.() -> Unit) = ProtoBuilder().apply(block).build()

    private class ProtoBuilder {

        private val buf = mutableListOf<Byte>()

        fun string(field: Int, value: String) {

            if (value.isNotEmpty()) bytes(field, value.toByteArray(Charsets.UTF_8))
        }

        fun embedded(field: Int, value: ByteArray) = bytes(field, value)

        fun bytes(
            field: Int,
            value: ByteArray
        ) {

            tag(field, 2)
            varint(value.size.toLong())
            buf.addAll(value.toList())
        }

        fun uint64(
            field: Int,
            value: Long
        ) {

            if (value != 0L) {
                tag(field, 0)
                varint(value)
            }
        }

        private fun tag(
            field: Int,
            wireType: Int
        ) = varint(((field shl 3) or wireType).toLong())

        private fun varint(v: Long) {

            var x = v

            while (x > 0x7F) {

                buf.add(((x and 0x7F) or 0x80).toByte())
                x = x ushr 7
            }

            buf.add((x and 0x7F).toByte())
        }

        fun build() = buf.toByteArray()
    }

    private val BECH32_CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"

    companion object {

        private const val PAYMENT_CODESPACE = "payment"
        private const val DUPLICATE_ORDER_ID_CODE = 2
        private const val SDK_CODESPACE = "sdk"
        private const val WRONG_SEQUENCE_CODE = 32
        private const val COMMIT_POLL_MAX_ATTEMPTS = 60
        private const val COMMIT_POLL_INTERVAL_MILLIS = 1000L
    }

    private fun convertBits(
        data: ByteArray,
        from: Int,
        to: Int
    ): List<Int> {

        var acc = 0
        var bits = 0
        val result = mutableListOf<Int>()
        val maxv = (1 shl to) - 1

        for (b in data) {

            acc = ((acc shl from) or (b.toInt() and 0xff))
            bits += from

            while (bits >= to) {

                bits -= to
                result.add((acc shr bits) and maxv)
            }
        }

        if (bits > 0) result.add((acc shl (to - bits)) and maxv)

        return result
    }

    private fun bech32Checksum(
        hrp: String,
        data: List<Int>
    ): String {

        val gen = intArrayOf(
            0x3b6a57b2,
            0x26508e6d,
            0x1ea119fa,
            0x3d4233dd,
            0x2a1462b3
        )
        val values = hrp.map { it.code shr 5 } +
                listOf(0) +
                hrp.map { it.code and 31 } +
                data +
                List(6) { 0 }
        var chk = 1

        for (v in values) {

            val b = chk shr 25
            chk = (chk and 0x1ffffff) shl 5 xor v

            for (i in 0..4) {

                if ((b shr i) and 1 == 1) chk = chk xor gen[i]
            }
        }

        val poly = chk xor 1

        return (0..5).map { BECH32_CHARSET[(poly shr (5 * (5 - it))) and 31] }.joinToString("")
    }
}