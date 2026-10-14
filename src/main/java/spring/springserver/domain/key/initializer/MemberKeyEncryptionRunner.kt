package spring.springserver.domain.key.initializer

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import spring.springserver.domain.key.repository.KeyRepository
import spring.springserver.domain.key.support.MemberKeyCipher
import spring.springserver.global.config.MemberKeyEncryptionProperties

/**
 * member-key.encrypt-plaintext가 켜져 있으면 평문으로 남아 있는 회원 개인키를 기동 시 일괄 암호화한다.
 *
 * 저장(saveAll)이 커밋된 뒤에야 평문 키를 막아야 그 사이 들어온 서명 요청이 실패하지 않으므로,
 * run 전체를 트랜잭션으로 묶지 않고 saveAll의 트랜잭션이 끝난 뒤 평문 차단을 건다.
 * 암호화 도중 예외가 나면 기동이 실패하고, 헬스체크에서 걸려 기존 컨테이너가 그대로 남는다.
 * 일괄 암호화가 끝난 뒤에는 조회 결과가 비어 저장도 일어나지 않는다.
 */
@Component
class MemberKeyEncryptionRunner(
    private val keyRepository: KeyRepository,
    private val memberKeyCipher: MemberKeyCipher,
    private val memberKeyEncryptionProperties: MemberKeyEncryptionProperties
): ApplicationRunner {

    override fun run(args: ApplicationArguments) {

        if (!memberKeyEncryptionProperties.encryptPlaintext) {

            return
        }

        val plainMemberKeys = keyRepository.findAllPlaintext()

        plainMemberKeys.forEach { memberKey ->

            val memberId = memberKey.getId()
                ?: throw IllegalStateException("Member key without member id cannot be encrypted.")

            memberKey.updatePrivateKey(
                memberKeyCipher.encrypt(
                    memberKeyCipher.decrypt(
                        memberKey.getPrivateKey(),
                        memberId
                    ),
                    memberId
                )
            )
        }

        if (plainMemberKeys.isNotEmpty()) keyRepository.saveAll(plainMemberKeys)

        memberKeyCipher.blockPlaintext()

        log.info("Encrypted {} plaintext member private key(s).", plainMemberKeys.size)
    }

    companion object {

        private val log = LoggerFactory.getLogger(MemberKeyEncryptionRunner::class.java)
    }
}