package spring.springserver.support

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * 실제 PostgreSQL·Redis를 컨테이너로 띄워 애플리케이션 전체를 올리는 통합 테스트의 기반.
 *
 * 컨테이너는 테스트 클래스마다 새로 띄우지 않고 JVM에서 한 번만 띄워 함께 쓴다.
 * 그래서 테스트끼리 데이터가 남으므로 각 테스트는 겹치지 않는 이메일·username을 쓴다.
 * 외부 서비스(메일, SMS, 토스, Cosmos, Agora)는 띄우지 않으므로 그 호출이 필요한 흐름은 다루지 않는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class IntegrationTest {

    companion object {

        private val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:17"))
            .withDatabaseName("idta")
            .withUsername("idta")
            .withPassword("idta")

        private val redis = GenericContainer(DockerImageName.parse("redis:7"))
            .withExposedPorts(6379)

        init {

            postgres.start()
            redis.start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun containerProperties(
            registry: DynamicPropertyRegistry
        ) {

            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.data.redis.host", redis::getHost)
            registry.add("spring.data.redis.port") { redis.getMappedPort(6379) }
        }
    }
}
