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

        if (!isEncrypted(encryptedPrivateKey)) throw ApplicationException(KeyStatusCode.PRIVATE_KEY_DECRYPTION_FAILED)

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
    }
}