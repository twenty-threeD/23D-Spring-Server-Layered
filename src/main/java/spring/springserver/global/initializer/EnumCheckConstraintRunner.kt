package spring.springserver.global.initializer

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.chat.data.response.ChatMessageType
import spring.springserver.domain.notification.entity.NotificationType
import spring.springserver.domain.payment.entity.PaymentStatus

/**
 * `ddl-auto: update`는 enum 컬럼의 체크 제약을 처음 만들 때만 생성하고 이후 갱신하지 않는다.
 * 그래서 나중에 추가한 enum 값(결제 CANCEL_REQUESTED, 알림 CALL_MISSED, 채팅 CALL 등)이 제약에 없어 그 행 저장이 실패한다.
 *
 * 기동 시 각 제약이 enum의 모든 값을 허용하는지 확인하고, 빠진 값이 있을 때만 다시 만든다.
 * 이미 맞으면 아무것도 하지 않으므로 재기동해도 안전하다.
 * enum 컬럼에 값을 추가했다면 아래 목록에 그 컬럼이 있는지 확인한다.
 */
@Component
class EnumCheckConstraintRunner(
    private val jdbcTemplate: JdbcTemplate
) : ApplicationRunner {

    private data class EnumCheckConstraint(
        val table: String,
        val column: String,
        val constraintName: String,
        val values: List<String>
    )

    companion object {

        private val log = LoggerFactory.getLogger(EnumCheckConstraintRunner::class.java)

        private val CONSTRAINTS = listOf(
            EnumCheckConstraint(
                table = "payment",
                column = "status",
                constraintName = "payment_status_check",
                values = PaymentStatus.entries.map { it.name }
            ),
            EnumCheckConstraint(
                table = "notification",
                column = "type",
                constraintName = "notification_type_check",
                values = NotificationType.entries.map { it.name }
            ),
            EnumCheckConstraint(
                table = "chat_message",
                column = "message_type",
                constraintName = "chat_message_message_type_check",
                values = ChatMessageType.entries.map { it.name }
            )
        )
    }

    @Transactional(rollbackFor = [Exception::class])
    override fun run(
        args: ApplicationArguments
    ) {

        CONSTRAINTS.forEach { constraint -> refreshIfOutdated(constraint) }
    }

    private fun refreshIfOutdated(
        constraint: EnumCheckConstraint
    ) {

        val definition = jdbcTemplate.queryForList(
            "select pg_get_constraintdef(oid) from pg_constraint " +
                    "where conrelid = to_regclass(?) and conname = ?",
            String::class.java,
            constraint.table,
            constraint.constraintName
        )
            .firstOrNull()

        if (definition != null && constraint.values.all { value -> definition.contains("'$value'") }) {

            return
        }

        if (jdbcTemplate.queryForObject("select to_regclass(?) is not null", Boolean::class.java, constraint.table) != true) {

            return
        }

        val allowed = constraint.values.joinToString(", ") { value -> "'$value'" }

        jdbcTemplate.execute("alter table ${constraint.table} drop constraint if exists ${constraint.constraintName}")
        jdbcTemplate.execute("alter table ${constraint.table} add constraint ${constraint.constraintName} check (${constraint.column} in ($allowed))")

        log.info(
            "{} 체크 제약을 갱신했습니다. allowed = {}",
            constraint.constraintName,
            allowed
        )
    }
}
