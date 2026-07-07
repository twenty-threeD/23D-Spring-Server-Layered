package spring.springserver.domain.key.service.impl

import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.crypto.digests.RIPEMD160Digest
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECPrivateKeySpec
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.key.entity.MemberKey
import spring.springserver.domain.key.repository.KeyRepository
import spring.springserver.domain.key.service.KeyService
import spring.springserver.domain.member.exception.MemberStatusCode
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.global.exception.exception.ApplicationException
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Signature
import java.security.interfaces.ECPrivateKey
import java.security.spec.ECGenParameterSpec
import java.util.Base64

@Service
@Transactional(rollbackFor = [Exception::class])
class KeyServiceImpl(
    private val keyRepository: KeyRepository,
    private val memberRepository: MemberRepository
): KeyService {

    override fun generateKeyPair(memberId: Long) {

        val member = memberRepository.findMemberById(memberId)
            ?: throw ApplicationException(MemberStatusCode.MEMBER_NOT_FOUND)

        val keyPair = KeyPairGenerator.getInstance("EC", BouncyCastleProvider())
            .also { it.initialize(ECGenParameterSpec("secp256k1"), SecureRandom()) }
            .generateKeyPair()

        val privateKey = (keyPair.private as ECPrivateKey).s.toString(16).padStart(64, '0')
        val publicKey = (keyPair.public as BCECPublicKey).q.getEncoded(true)
            .joinToString("") { "%02x".format(it) }

        keyRepository.save(
            MemberKey(
                member = member,
                privateKey = privateKey,
                publicKey = publicKey
            )
        )
    }

    override fun signHash(
        memberId: Long,
        hash: String
    ): String {

        val memberKey =keyRepository.findByMemberId(memberId = memberId)
            ?: throw ApplicationException(MemberStatusCode.MEMBER_NOT_FOUND)

        val privateKey = restorePrivateKey(memberKey.getPrivateKey())
        val signer = Signature.getInstance("SHA256withECDSA", BouncyCastleProvider())

        signer.initSign(privateKey)
        signer.update(hash.toByteArray(Charsets.UTF_8))

        val derSig = signer.sign()

        val seq = ASN1Sequence.getInstance(derSig)
        val r = (seq.getObjectAt(0) as ASN1Integer).positiveValue
        var s = (seq.getObjectAt(1) as ASN1Integer).positiveValue
        val n = BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16)

        if (s > n.shiftRight(1)) s = n.subtract(s)

        val sig64 = ByteArray(64)
        val rBytes = r.toByteArray().let { if (it.size == 33) it.copyOfRange(1, 33) else it }
        val sBytes = s.toByteArray().let { if (it.size == 33) it.copyOfRange(1, 33) else it }

        rBytes.copyInto(sig64, 32 - rBytes.size)
        sBytes.copyInto(sig64, 64 - sBytes.size)

        return Base64.getEncoder().encodeToString(sig64)
    }

    override fun deriveCosmosAddress(
        memberId: Long
    ): String {

        val memberKey = keyRepository.findByMemberId(memberId = memberId)
            ?: throw ApplicationException(MemberStatusCode.MEMBER_NOT_FOUND)

        val pubKeyBytes = memberKey.getPublicKey()
            .chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()

        val sha256 = MessageDigest.getInstance("SHA-256").digest(pubKeyBytes)
        val ripemd160 = ByteArray(20)
        RIPEMD160Digest().apply {

            update(
                sha256,
                0,
                sha256.size
            )
            doFinal(
                ripemd160,
                0
            )
        }

        val converted = convertBits(
            ripemd160,
            8,
            5
        )

        return "cosmos1" + converted.joinToString("") { BECH32_CHARSET[it].toString() } +
                bech32Checksum(
                    "cosmos",
                    converted
                )
    }

    override fun getPublicKeyBase64(
        memberId: Long
    ): String {

        val memberKey = keyRepository.findByMemberId(memberId = memberId)
            ?: throw ApplicationException(MemberStatusCode.MEMBER_NOT_FOUND)

        return Base64.getEncoder().encodeToString(
            memberKey.getPublicKey()
                .chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
        )
    }

    private val BECH32_CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"

    private fun convertBits(
        data: ByteArray, fromBits: Int,
        toBits: Int
    ): List<Int> {

        var acc = 0
        var bits = 0
        val result = mutableListOf<Int>()
        val maxv = (1 shl toBits) - 1

        for (b in data) {

            acc = ((acc shl fromBits) or (b.toInt() and 0xff))
            bits += fromBits
            while (bits >= toBits) {

                bits -= toBits
                result.add((acc shr bits) and maxv)
            }
        }

        if (bits > 0) result.add((acc shl (toBits  - bits)) and maxv)

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
                listOf(0, 0, 0, 0, 0, 0)
        var chk = 1

        for (v in values) {

            val b = chk shr 25
            chk = (chk and 0x1ffffff) shl 5 xor v

            for (i in 0..4) {

                if ((b shr i) and 1 == 1) { chk = chk xor gen[i] }
            }
        }
        val polymod = chk xor 1

        return (0..5).map { BECH32_CHARSET[(polymod shr (5 * (5 - it))) and 31] }.joinToString("")
    }

    private fun restorePrivateKey(privateKeyHex: String): PrivateKey {

        return KeyFactory.getInstance(
            "EC",
            BouncyCastleProvider()
        ).generatePrivate(
            ECPrivateKeySpec(
                BigInteger(
                    privateKeyHex,
                    16
                ),
                ECNamedCurveTable.getParameterSpec("secp256k1")
            )
        )
    }
}