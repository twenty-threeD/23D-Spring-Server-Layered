package spring.springserver.domain.member.retention

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.member.repository.MemberRepository
import java.time.LocalDateTime

/**
 * 탈퇴 회원의 재가입 제한을 관리한다.
 *
 * 탈퇴해도 행은 남고 username·email·phone의 unique 제약도 그대로라 재가입이 막힌다.
 * 제한 기간(RETENTION_DAYS)이 지나면 이 서비스가 식별자를 치환해 값을 풀어준다.
 */
@Service
class MemberRetentionService(
    private val memberRepository: MemberRepository
) {

    private val log = LoggerFactory.getLogger(MemberRetentionService::class.java)

    /**
     * 매일 새벽 4시 30분에 제한 기간이 지난 탈퇴 회원을 익명화한다.
     * 커뮤니티 콘텐츠 정리(4시)와 겹치지 않게 시간을 벌려 둔다.
     */
    @Scheduled(cron = "0 30 4 * * *")
    @Transactional(rollbackFor = [Exception::class])
    fun anonymizeExpiredMembers() {

        val expiredMembers = memberRepository
            .findAllByDeletedAtBeforeAndAnonymizedAtIsNull(threshold())

        if (expiredMembers.isEmpty()) {

            return
        }

        expiredMembers.forEach { member -> member.anonymize() }

        memberRepository.saveAll(expiredMembers)

        log.info("재가입 제한 기간이 지난 탈퇴 회원 {}건 익명화", expiredMembers.size)
    }

    /**
     * 재가입에 쓰려는 값을 쥐고 있는 탈퇴 회원을 제한 기간이 지났으면 그 자리에서 익명화한다.
     * 스케줄러가 아직 돌지 않았어도 기간만 지났다면 바로 가입할 수 있어야 하기 때문이다.
     *
     * 새 트랜잭션에서 처리하고 즉시 flush한다.
     * 같은 트랜잭션에서 처리하면 Hibernate가 insert를 update보다 먼저 내보내
     * 아직 풀리지 않은 unique 값과 부딪히고, 조회 전용 트랜잭션(중복 확인)에서는 쓰기가 막힌다.
     *
     * @return 값이 풀려 재가입할 수 있으면 true, 아직 제한 기간 중이면 false
     */
    @Transactional(
        propagation = Propagation.REQUIRES_NEW,
        rollbackFor = [Exception::class]
    )
    fun releaseIfRetentionExpired(
        memberId: Long
    ): Boolean {

        val member = memberRepository.findMemberById(memberId)
            ?: return true

        if (!member.isDeleted()) {

            return false
        }

        if (member.isAnonymized()) {

            return true
        }

        if (!member.isWithdrawnBefore(threshold())) {

            return false
        }

        member.anonymize()

        memberRepository.saveAndFlush(member)

        return true
    }

    private fun threshold(): LocalDateTime {

        return LocalDateTime.now().minusDays(RETENTION_DAYS)
    }

    companion object {

        const val RETENTION_DAYS = 30L
    }
}
