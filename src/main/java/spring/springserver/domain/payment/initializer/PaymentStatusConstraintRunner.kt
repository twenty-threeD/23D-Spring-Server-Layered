package spring.springserver.domain.payment.initializer

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.payment.entity.PaymentStatus

/**
 * `ddl-auto: update`는 enum 컬럼의 체크 제약을 처음 만들 때만 생성하고 이후 갱신하지 않는다.
 * 그래서 나중에 추가한 PARTIAL_CANCELED·CANCELED가 제약에 없어 취소 상태 저장이 항상 실패한다.
 *
 * 기동 시 제약이 PaymentStatus의 모든 값을 허용하는지 확인하고, 빠진 값이 있을 때만 다시 만든다.
 * 이미 맞으면 아무것도 하지 않으므로 재기동해도 안전하다.
 */
@Component
class PaymentStatusConstraintRunner(
    private val jdbcTemplate: JdbcTemplate
) : ApplicationRunner {

    companion object {

        private val log = LoggerFactory.getLogger(PaymentStatusConstraintRunner::class.java)

        private const val CONSTRAINT_NAME = "payment_status_check"
    }

    @Transactional(rollbackFor = [Exception::class])
    override fun run(
        args: ApplicationArguments
    ) {

        val definition = jdbcTemplate.queryForList(
            "select pg_get_constraintdef(oid) from pg_constraint " +
                    "where conrelid = to_regclass('payment') and conname = ?",
            String::class.java,
            CONSTRAINT_NAME
        )
            .firstOrNull()

        val statuses = PaymentStatus.entries.map { it.name }

        if (definition != null && statuses.all { status -> definition.contains("'$status'") }) {

            return
        }

        if (jdbcTemplate.queryForObject("select to_regclass('payment') is not null", Boolean::class.java) != true) {

            return
        }

        val allowed = statuses.joinToString(", ") { status -> "'$status'" }

        jdbcTemplate.execute("alter table payment drop constraint if exists $CONSTRAINT_NAME")
        jdbcTemplate.execute("alter table payment add constraint $CONSTRAINT_NAME check (status in ($allowed))")

        log.info("payment 상태 체크 제약을 갱신했습니다. allowed = {}", allowed)
    }
}