package spring.springserver.domain.profile.initializer

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.domain.profile.entity.Profile
import spring.springserver.domain.profile.repository.ProfileRepository

@Component
class ProfileBackfillRunner(
    private val memberRepository: MemberRepository,
    private val profileRepository: ProfileRepository
) : ApplicationRunner {

    companion object {

        private val log = LoggerFactory.getLogger(ProfileBackfillRunner::class.java)
    }

    @Transactional
    override fun run(args: ApplicationArguments) {

        var createdCount = 0

        memberRepository.findAll().forEach {

            member ->
            if (!profileRepository.existsByMember(member)) {
                profileRepository.save(Profile(member = member))
                createdCount++
            }
        }

        if (createdCount > 0) {

            log.info("Backfilled {} missing profile(s).", createdCount)
        }
    }
}
