package spring.springserver.domain.blockchain.service

import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.crypto.digests.RIPEMD160Digest
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECPrivateKeySpec
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import spring.springserver.global.config.blockchain.CosmosProperties
import java.math.BigInteger
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.util.Base64

@Service
class BlockchainService(
    private val cosmosProperties: CosmosProperties
) {

    private val restTemplate = RestTemplate()

    private val submitterPubKeyBytes: ByteArray by lazy {

        ECNamedCurveTable.getParameterSpec("secp256k1").g
            .multiply(BigInteger(cosmosProperties.submitterPrivateKey, 16))
            .normalize()
            .getEncoded(true)
    }

    val submitterAddress: String by lazy {

        deriveCosmosAddress(submitterPubKeyBytes)
    }

    fun recordPayment(
        buyerAddress: String,
        buyerPubKeyBase64: String,
        submissionId: String,
        hash: String,
        buyerSignature: String
    ) {

        val (accountNumber, sequence) = getAccountInfo(submitterAddress)

        val msgBytes = buildProto {
            string(1, submitterAddress)
            string(2, buyerAddress)
            string(3, buyerPubKeyBase64)
            string(4, submissionId)
            string(5, hash)
            string(6, buyerSignature)
        }

        val anyBytes = buildProto {
            string(1, "/cosmos.staking.v1beta1.MsgRecordPayment")
            bytes(2, msgBytes)
        }

        val txBodyBytes = buildProto {
            embedded(1, anyBytes)
            string(2, "")
        }

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
            uint64(4, accountNumber)
            // Sequence intentionally omitted: this fork's GetSignBytes excludes it
        }

        val signature = signBytes(signDocBytes)

        val txRawBytes = buildProto {
            bytes(1, txBodyBytes)
            bytes(2, authInfoBytes)
            bytes(3, signature)
        }

        broadcast(Base64.getEncoder().encodeToString(txRawBytes))
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

    private fun broadcast(txBase64: String) {

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
        val code = txResponse?.get("code")?.toString()?.toInt() ?: -1

        if (code != 0) error("chain rejected tx (code=$code): ${txResponse?.get("raw_log")}")
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