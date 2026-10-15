package spring.springserver.domain.key.support

import org.springframework.stereotype.Component
import spring.springserver.domain.key.exception.KeyStatusCode
import spring.springserver.global.config.MemberKeyEncryptionProperties
import spring.springserver.global.exception.exception.ApplicationException
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 회원 서명용 개인키를 AES-256-GCM으로 암호화/복호화한다.
 *
 * 저장 형식은 "v1:" + Base64(nonce || ciphertext || tag)이며, 회원 ID를 AAD로 묶어
 * 암호문을 다른 회원 행으로 옮겨 붙이면 복호화되지 않게 한다.
 * 마스터키가 없거나 길이가 맞지 않으면 평문으로 넘어가지 않도록 기동 자체를 실패시킨다.
 *
 * 기존 평문 키를 일괄 암호화하기 전까지는 Blue-Green 전환 중에도 서명이 끊기지 않도록
 * 접두사가 없는 값을 평문 키로 읽는다. member-key.encrypt-plaintext가 켜져 있으면
 * 일괄 암호화가 커밋된 뒤부터 평문 키를 거부해, DB에 평문 키를 바꿔 넣는 공격을 막는다.
 */
@Component
class MemberKeyCipher(
    memberKeyEncryptionProperties: MemberKeyEncryptionProperties
) {

    private val secretKey = SecretKeySpec(
        decodeMasterKey(memberKeyEncryptionProperties.masterKey),
        "AES"
    )
    private val secureRandom = SecureRandom()

    @Volatile
    private var plaintextReadable = true

    fun encrypt(
        privateKey: String,
        memberId: Long
    ): String {

        val nonce = ByteArray(NONCE_LENGTH).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)

        cipher.init(
            Cipher.ENCRYPT_MODE,
            secretKey,
            GCMParameterSpec(
                TAG_LENGTH_BITS,
                nonce
            )
        )
        cipher.updateAAD(memberId.toString().toByteArray(Charsets.UTF_8))

        val encrypted = cipher.doFinal(privateKey.toByteArray(Charsets.UTF_8))

        return VERSION_PREFIX + Base64.getEncoder().encodeToString(nonce + encrypted)
    }

    fun decrypt(
        encryptedPrivateKey: String,
        memberId: Long
    ): String {

        if (!isEncrypted(encryptedPrivateKey)) {

            return readPlaintextPrivateKey(encryptedPrivateKey)
        }

        return runCatching {

            val payload = Base64.getDecoder().decode(encryptedPrivateKey.removePrefix(VERSION_PREFIX))
            val cipher = Cipher.getInstance(TRANSFORMATION)

            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey,
                GCMParameterSpec(
                    TAG_LENGTH_BITS,
                    payload,
                    0,
                    NONCE_LENGTH
                )
            )
            cipher.updateAAD(memberId.toString().toByteArray(Charsets.UTF_8))

            String(
                cipher.doFinal(
                    payload,
                    NONCE_LENGTH,
                    payload.size - NONCE_LENGTH
                ),
                Charsets.UTF_8
            )
        }.getOrElse { throw ApplicationException(KeyStatusCode.PRIVATE_KEY_DECRYPTION_FAILED) }
    }

    fun isEncrypted(
        privateKey: String
    ): Boolean {

        return privateKey.startsWith(VERSION_PREFIX)
    }

    /**
     * 평문 개인키 일괄 암호화가 커밋된 뒤 호출해, 이후 평문 개인키를 읽지 않도록 막는다.
     */
    fun blockPlaintext() {

        plaintextReadable = false
    }

    private fun readPlaintextPrivateKey(
        privateKey: String
    ): String {

        if (!plaintextReadable) throw ApplicationException(KeyStatusCode.PRIVATE_KEY_DECRYPTION_FAILED)

        if (!PLAINTEXT_PRIVATE_KEY_PATTERN.matches(privateKey)) throw ApplicationException(KeyStatusCode.PRIVATE_KEY_DECRYPTION_FAILED)

        return privateKey
    }

    private fun decodeMasterKey(
        masterKey: String
    ): ByteArray {

        val decoded = runCatching { Base64.getDecoder().decode(masterKey.trim()) }
            .getOrElse { throw IllegalStateException("member-key.master-key is not valid Base64.") }

        if (decoded.size != MASTER_KEY_LENGTH) throw IllegalStateException("member-key.master-key must be 32 bytes.")

        return decoded
    }

    companion object {

        const val VERSION_PREFIX = "v1:"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val MASTER_KEY_LENGTH = 32
        private const val NONCE_LENGTH = 12
        private const val TAG_LENGTH_BITS = 128
        private val PLAINTEXT_PRIVATE_KEY_PATTERN = Regex("^[0-9a-f]{64}$")
    }
}