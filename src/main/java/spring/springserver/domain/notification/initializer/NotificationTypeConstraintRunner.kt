package spring.springserver.domain.notification.initializer

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.notification.entity.NotificationType

/**
 * `ddl-auto: update`는 enum 컬럼의 체크 제약을 처음 만들 때만 생성하고 이후 갱신하지 않는다.
 * 그래서 나중에 추가한 NotificationType 값(CALL_MISSED 등)이 제약에 없어 그 알림 저장이 실패한다.
 *
 * 기동 시 제약이 NotificationType의 모든 값을 허용하는지 확인하고, 빠진 값이 있을 때만 다시 만든다.
 * 이미 맞으면 아무것도 하지 않으므로 재기동해도 안전하다. 같은 방식의 결제 쪽 러너는 PaymentStatusConstraintRunner다.
 */
@Component
class NotificationTypeConstraintRunner(
    private val jdbcTemplate: JdbcTemplate
) : ApplicationRunner {

    companion object {

        private val log = LoggerFactory.getLogger(NotificationTypeConstraintRunner::class.java)

        private const val CONSTRAINT_NAME = "notification_type_check"
    }

    @Transactional(rollbackFor = [Exception::class])
    override fun run(
        args: ApplicationArguments
    ) {

        val definition = jdbcTemplate.queryForList(
            "select pg_get_constraintdef(oid) from pg_constraint " +
                    "where conrelid = to_regclass('notification') and conname = ?",
            String::class.java,
            CONSTRAINT_NAME
        )
            .firstOrNull()

        val types = NotificationType.entries.map { it.name }

        if (definition != null && types.all { type -> definition.contains("'$type'") }) {

            return
        }

        if (jdbcTemplate.queryForObject("select to_regclass('notification') is not null", Boolean::class.java) != true) {

            return
        }

        val allowed = types.joinToString(", ") { type -> "'$type'" }

        jdbcTemplate.execute("alter table notification drop constraint if exists $CONSTRAINT_NAME")
        jdbcTemplate.execute("alter table notification add constraint $CONSTRAINT_NAME check (type in ($allowed))")

        log.info("notification 종류 체크 제약을 갱신했습니다. allowed = {}", allowed)
    }
}