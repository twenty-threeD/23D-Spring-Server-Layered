package spring.springserver.support

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import redis.embedded.RedisServer
import spring.springserver.domain.member.initializer.MemberEmailIndexRunner
import spring.springserver.global.initializer.EnumCheckConstraintRunner
import java.net.ServerSocket

/**
 * 인메모리 H2(PostgreSQL 호환 모드)와 내장 Redis로 애플리케이션 전체를 올리는 통합 테스트의 기반.
 * Docker 없이 JVM 안에서만 돌아가므로 CI와 로컬 어디서든 그대로 실행된다.
 *
 * Redis는 테스트 클래스마다 새로 띄우지 않고 JVM에서 한 번만 띄워 함께 쓰고, H2도 같은 이름의 DB를 쓴다.
 * 그래서 테스트끼리 데이터가 남으므로 각 테스트는 겹치지 않는 이메일·username을 쓴다.
 * 외부 서비스(메일, SMS, 토스, Cosmos, Agora)는 띄우지 않으므로 그 호출이 필요한 흐름은 다루지 않는다.
 *
 * H2는 PostgreSQL이 아니므로 다음은 실제와 다르게 동작한다.
 * - pg_advisory_xact_lock, hashtext는 H2Functions의 빈 함수로 대신한다. 동시성 잠금은 이 테스트로 검증할 수 없다.
 * - EnumCheckConstraintRunner는 PostgreSQL 시스템 카탈로그를, MemberEmailIndexRunner는 to_regclass를 쓰므로 목으로 바꿔 실행하지 않는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class IntegrationTest {

    @MockitoBean
    private lateinit var enumCheckConstraintRunner: EnumCheckConstraintRunner

    @MockitoBean
    private lateinit var memberEmailIndexRunner: MemberEmailIndexRunner

    companion object {

        private val redisPort = ServerSocket(0).use { it.localPort }

        private val redis = RedisServer(redisPort)

        init {

            redis.start()
            Runtime.getRuntime().addShutdownHook(Thread { redis.stop() })
        }

        @JvmStatic
        @DynamicPropertySource
        fun inMemoryProperties(
            registry: DynamicPropertyRegistry
        ) {

            registry.add("spring.data.redis.host") { "localhost" }
            registry.add("spring.data.redis.port") { redisPort }
        }
    }
}