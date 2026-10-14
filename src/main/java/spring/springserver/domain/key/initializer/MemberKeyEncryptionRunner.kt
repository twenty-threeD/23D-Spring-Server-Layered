package spring.springserver.domain.key.initializer

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.key.repository.KeyRepository
import spring.springserver.domain.key.support.MemberKeyCipher

/**
 * 평문으로 저장돼 있던 회원 개인키를 기동 시 한 번 암호화해 둔다.
 *
 * 암호화 접두사가 없는 행만 DB에서 골라오므로, 마이그레이션이 끝난 뒤에는 조회 결과가 비어 저장도 일어나지 않는다.
 */
@Component
class MemberKeyEncryptionRunner(
    private val keyRepository: KeyRepository,
    private val memberKeyCipher: MemberKeyCipher
): ApplicationRunner {

    @Transactional
    override fun run(args: ApplicationArguments) {

        val plainMemberKeys = keyRepository.findAllByPrivateKeyNotStartingWith(MemberKeyCipher.VERSION_PREFIX)

        if (plainMemberKeys.isEmpty()) {

            return
        }

        plainMemberKeys.forEach { memberKey ->

            val memberId = memberKey.getId()
                ?: return@forEach

            memberKey.updatePrivateKey(
                memberKeyCipher.encrypt(
                    memberKey.getPrivateKey(),
                    memberId
                )
            )
        }

        log.info("Encrypted {} plaintext member private key(s).", plainMemberKeys.size)
    }

    companion object {

        private val log = LoggerFactory.getLogger(MemberKeyEncryptionRunner::class.java)
    }
}