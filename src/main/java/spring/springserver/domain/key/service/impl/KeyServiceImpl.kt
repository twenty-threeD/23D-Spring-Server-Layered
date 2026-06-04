package spring.springserver.domain.key.service.impl

import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.key.entity.MemberKey
import spring.springserver.domain.key.repository.KeyRepository
import spring.springserver.domain.key.service.KeyService
import spring.springserver.domain.member.exception.MemberStatusCode
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.global.exception.exception.ApplicationException
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.interfaces.ECPrivateKey
import java.security.spec.ECGenParameterSpec

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

        val privateKey = (keyPair.private as ECPrivateKey).s.toString(16)
        val publicKey = (keyPair.public as BCECPublicKey).q.getEncoded(false)
            .joinToString("") { "%02x".format(it) }

        keyRepository.save(
            MemberKey(
                member = member,
                privateKey = privateKey,
                publicKey = publicKey
            )
        )
    }
}