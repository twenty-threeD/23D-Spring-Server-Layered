package spring.springserver.domain.profile.initializer

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.profile.entity.Profile
import spring.springserver.domain.profile.repository.ProfileRepository

/**
 * 프로필 행이 없는 회원의 프로필을 기동 시 한 번 만들어 둔다.
 *
 * 회원을 전부 메모리로 올린 뒤 한 명씩 존재 여부를 조회하면 회원 수만큼 쿼리가 나가고
 * 그만큼 기동이 늦어지므로, 빠진 회원을 골라내는 일을 DB에 맡겨 조회 한 번으로 끝낸다.
 * 백필이 끝난 뒤에는 조회 결과가 비어 있어 저장도 일어나지 않는다.
 */
@Component
class ProfileBackfillRunner(
    private val profileRepository: ProfileRepository
): ApplicationRunner {

    @Transactional
    override fun run(args: ApplicationArguments) {

        val membersWithoutProfile = profileRepository.findMembersWithoutProfile()

        if (membersWithoutProfile.isEmpty()) {

            return
        }

        profileRepository.saveAll(
            membersWithoutProfile.map { member -> Profile(member = member) }
        )

        log.info("Backfilled {} missing profile(s).", membersWithoutProfile.size)
    }

    companion object {

        private val log = LoggerFactory.getLogger(ProfileBackfillRunner::class.java)
    }
}