package spring.springserver.domain.auth

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.support.IntegrationTest
import java.util.UUID
import java.util.concurrent.TimeUnit

@DisplayName("인증 흐름 통합 테스트")
class AuthFlowIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val redisTemplate: RedisTemplate<String, String>,
    @Autowired private val memberRepository: MemberRepository
) : IntegrationTest() {

    companion object {

        private const val PASSWORD = "Passw0rd!"
    }

    @Test
    @DisplayName("이메일 인증 없이 가입하면 거부한다")
    fun signUpWithoutEmailVerification() {

        val username = newUsername()

        val response = signUp(
            username,
            "$username@test.com"
        )

        assertThat(response.path("status").asInt()).isEqualTo(400)
        assertThat(response.path("error").path("code").asText()).isEqualTo("EMAIL_NOT_VERIFIED")
    }

    @Test
    @DisplayName("가입 후 로그인하면 토큰으로 인증 API를 쓸 수 있다")
    fun signUpAndSignIn() {

        val username = newUsername()
        val email = "$username@test.com"

        markEmailVerified(email)

        assertThat(
            signUp(
                username,
                email
            ).path("status").asInt()
        ).isEqualTo(200)

        assertThat(memberRepository.findByUsername(username)).isNotNull

        val tokens = signIn(email)

        mockMvc.get("/api/token/username") {

            header("Authorization", "Bearer ${tokens.path("accessToken").asText()}")
        }.andExpect {

            status { isOk() }
            jsonPath("$.data.username") { value(username) }
        }
    }

    @Test
    @DisplayName("비밀번호가 틀리면 로그인을 거부한다")
    fun signInWithWrongPassword() {

        val username = newUsername()
        val email = "$username@test.com"

        markEmailVerified(email)
        signUp(
            username,
            email
        )

        mockMvc.post("/api/auth/signin") {

            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "email" to email,
                    "password" to "Wrong0rd!"
                )
            )
        }.andExpect {

            status { isUnauthorized() }
            jsonPath("$.error.code") { value("INVALID_CREDENTIALS") }
        }
    }

    @Test
    @DisplayName("refreshToken으로 accessToken을 다시 받는다")
    fun reissueAccessToken() {

        val username = newUsername()
        val email = "$username@test.com"

        markEmailVerified(email)
        signUp(
            username,
            email
        )

        val tokens = signIn(email)

        mockMvc.post("/api/token/reissue") {

            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("refreshToken" to tokens.path("refreshToken").asText()))
        }.andExpect {

            status { isOk() }
            jsonPath("$.data.accessToken") { isNotEmpty() }
        }
    }

    @Test
    @DisplayName("로그아웃한 accessToken은 더 이상 쓸 수 없다")
    fun signOutRevokesAccessToken() {

        val username = newUsername()
        val email = "$username@test.com"

        markEmailVerified(email)
        signUp(
            username,
            email
        )

        val accessToken = signIn(email).path("accessToken").asText()

        mockMvc.post("/api/auth/signout") {

            header("Authorization", "Bearer $accessToken")
        }.andExpect {

            status { isOk() }
        }

        mockMvc.get("/api/token/username") {

            header("Authorization", "Bearer $accessToken")
        }.andExpect {

            status { isUnauthorized() }
        }
    }

    private fun newUsername(): String = "it${UUID.randomUUID().toString().replace("-", "").take(12)}"

    /**
     * 실제 메일을 보내지 않고 인증코드 확인을 마친 상태를 만든다.
     */
    private fun markEmailVerified(email: String) {

        redisTemplate.opsForValue().set(
            "email:verified:${email.lowercase()}",
            "true",
            10,
            TimeUnit.MINUTES
        )
    }

    private fun signUp(
        username: String,
        email: String
    ): JsonNode {

        val body = mockMvc.post("/api/auth/signup") {

            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "name" to "테스트",
                    "username" to username,
                    "email" to email,
                    "password" to PASSWORD
                )
            )
        }.andReturn().response.getContentAsString(Charsets.UTF_8)

        return objectMapper.readTree(body)
    }

    private fun signIn(email: String): JsonNode {

        val body = mockMvc.post("/api/auth/signin") {

            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "email" to email,
                    "password" to PASSWORD
                )
            )
        }.andExpect {

            status { isOk() }
        }.andReturn().response.getContentAsString(Charsets.UTF_8)

        return objectMapper.readTree(body).path("data")
    }
}
