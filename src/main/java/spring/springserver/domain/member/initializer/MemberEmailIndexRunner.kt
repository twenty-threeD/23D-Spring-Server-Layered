package spring.springserver.domain.member.initializer

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

/**
 * 회원 이메일은 lower(email)로 비교해 찾으므로 email 컬럼 인덱스를 타지 못한다.
 * 로그인·가입마다 member 테이블 전체를 읽지 않도록 lower(email) 함수 인덱스를 둔다.
 *
 * `ddl-auto: update`는 함수 인덱스를 만들지 못해 기동 시 직접 만든다.
 * 대소문자만 다른 기존 중복이 있어도 기동이 막히지 않도록 유니크가 아닌 일반 인덱스로 만든다.
 * 이미 있으면 아무것도 하지 않으므로 재기동해도 안전하다.
 */
@Component
class MemberEmailIndexRunner(
    private val jdbcTemplate: JdbcTemplate
): ApplicationRunner {

    override fun run(
        args: ApplicationArguments
    ) {

        if (jdbcTemplate.queryForObject("select to_regclass('member') is not null", Boolean::class.java) != true) {

            return
        }

        jdbcTemplate.execute("create index if not exists idx_member_lower_email on member (lower(email))")
    }
}