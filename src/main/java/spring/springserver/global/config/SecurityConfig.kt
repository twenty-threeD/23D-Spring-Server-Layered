package spring.springserver.global.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.util.matcher.IpAddressMatcher
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter
import spring.springserver.domain.auth.handler.OAuth2FailureHandler
import spring.springserver.domain.auth.handler.OAuth2SuccessHandler
import spring.springserver.domain.auth.service.oauth.CustomOAuthUserService
import spring.springserver.global.handler.ApiAccessDeniedHandler
import spring.springserver.global.handler.ApiAuthenticationEntryPoint
import spring.springserver.global.jwt.JwtAuthFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    @param:Value($$"${app.cors.allowed-origins}") private val corsAllowedOrigins: String,
    private val jwtAuthFilter: JwtAuthFilter,
    private val cookieOAuth2AuthorizationRequestRepository: CookieOAuth2AuthorizationRequestRepository,
    private val apiAuthenticationEntryPoint: ApiAuthenticationEntryPoint,
    private val apiAccessDeniedHandler: ApiAccessDeniedHandler
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder {

        return BCryptPasswordEncoder()
    }

    /**
     * Actuator 전용 체인. @Order가 없는 filterChain은 LOWEST_PRECEDENCE라
     * 뒤로 밀리므로, 이 체인이 actuator 경로를 먼저 가져간다.
     *
     * 메인 체인에 규칙만 얹지 않고 체인을 분리한 이유:
     *   1. anyRequest().authenticated()에 걸려 스크레이프가 401로 실패한다.
     *   2. 스크레이프에는 JWT 필터도 OAuth2 로그인도 필요 없다.
     *
     * Prometheus는 컨테이너에서 host.docker.internal을 거쳐 오므로 remoteAddr이
     * 루프백이 아니라 Docker 브리지 대역(172.16.0.0/12)이다.
     * "localhost만 허용"으로 풀면 스크레이프가 막힌다.
     *
     * 주의: server.forward-headers-strategy=framework 때문에 X-Forwarded-For가
     * remoteAddr에 반영된다. 이 IP 검사만으로는 헤더 스푸핑을 막지 못하므로
     * nginx에서 /actuator를 차단하는 것이 1차 방어선이고 여기는 2차 방어선이다.
     */
    @Bean
    @Order(1)
    fun actuatorFilterChain(
        httpSecurity: HttpSecurity
    ): SecurityFilterChain {

        val allowedMatchers = listOf(
            IpAddressMatcher("127.0.0.0/8"),
            IpAddressMatcher("::1/128"),
            IpAddressMatcher("172.16.0.0/12")
        )

        httpSecurity
            .securityMatcher("/actuator/**")
            .httpBasic { httpBasic -> httpBasic.disable() }
            .formLogin { formLogin -> formLogin.disable() }
            .csrf { csrf -> csrf.disable() }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        HttpMethod.GET,
                        "/actuator/health"
                    ).permitAll()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/actuator/prometheus"
                    ).access { _, context ->
                        AuthorizationDecision(
                            allowedMatchers.any { matcher -> matcher.matches(context.request) }
                        )
                    }
                    .anyRequest()
                    .denyAll()
            }

        return httpSecurity.build()
    }

    @Bean
    fun filterChain(
        httpSecurity: HttpSecurity,
        customOAuthUserService: CustomOAuthUserService,
        oAuth2SuccessHandler: OAuth2SuccessHandler,
        oAuth2FailureHandler: OAuth2FailureHandler
    ): SecurityFilterChain {

        httpSecurity
            .httpBasic {
                httpBasic -> httpBasic.disable()
            }
            .formLogin { formLogin -> formLogin.disable() }
            .csrf { csrf -> csrf.disable() }
            .cors { }
            .exceptionHandling { exception ->
                exception
                    .authenticationEntryPoint(apiAuthenticationEntryPoint)
                    .accessDeniedHandler(apiAccessDeniedHandler)
            }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/auth/signup",
                        "/api/auth/signin",
                        "/api/auth/signout",
                        "/api/auth/password/reset",
                        "/api/auth/verify/password"
                    ).permitAll()
                    /**
                     * 소셜 로그인 시작(리다이렉트)과 provider 콜백은 로그인 전에 열려 있어야 한다.
                     * 실제 경로는 /oauth2/authorization/{provider}, /login/oauth2/code/{provider}이며
                     * 둘 다 GET이다.
                     */
                    .requestMatchers(
                        HttpMethod.GET,
                        "/oauth2/authorization/**",
                        "/login/oauth2/code/**"
                    ).permitAll()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/member/check-username",
                        "/api/member/check-email",
                        "/api/member/check-phone"
                    ).permitAll()
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/auth/password/reset/check"
                    ).hasRole("USER")
                    .requestMatchers(
                        HttpMethod.PATCH,
                        "/api/member/email",
                        "/api/member/phone"
                    ).hasRole("USER")
                    .requestMatchers(
                        HttpMethod.DELETE,
                        "/api/delete/account")
                    .hasRole("USER")
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/token/username"
                    ).permitAll()
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/token/reissue"
                    ).permitAll()
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/files/upload"
                    ).hasRole("USER")
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/post"
                    ).hasRole("USER")
                    .requestMatchers(
                        HttpMethod.PATCH,
                        "/api/post")
                    .hasRole("USER")
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/post/**",
                        "/api/job-category"
                    ).permitAll()
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/post/review"
                    ).authenticated()
                    .requestMatchers(
                        HttpMethod.PATCH,
                        "/api/post/review"
                    ).authenticated()
                    .requestMatchers(
                        HttpMethod.DELETE,
                        "/api/post/review"
                    ).authenticated()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/post/*"
                    ).permitAll()
                    .requestMatchers(
                        HttpMethod.DELETE,
                        "/api/post/*")
                    .hasRole("USER")
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/email/code/send",
                        "/api/email/code/verify"
                    ).permitAll()
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/email/change/code/send"
                    ).hasRole("USER")
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/payment/prepare",
                        "/api/payment/confirm",
                        "/api/payment/*/cancel",
                        "/api/payment/virtual-accounts"
                    ).hasRole("USER")
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/payment/*",
                        "/api/payment/orders/*",
                        "/api/payment/orders/*/verify",
                    ).hasRole("USER")
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/blockchain/verify/*"
                    ).permitAll()
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/estimate"
                    ).hasRole("USER")
                    .requestMatchers(
                        HttpMethod.PATCH,
                        "/api/estimate/*"
                    ).hasRole("USER")
                    .requestMatchers(
                        HttpMethod.DELETE,
                        "/api/estimate/*"
                    ).hasRole("USER")
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/estimate",
                        "/api/estimate/*"
                    ).hasRole("USER")
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/contract"
                    ).hasAnyRole("USER", "PROFESSIONAL")
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/contract/*"
                    ).hasAnyRole("USER", "PROFESSIONAL")
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/community/post/",
                        "/api/community/post/{postId}",
                        "/api/community/post/search",
                        "/api/community/post/category"
                    ).permitAll()
                    .requestMatchers(
                        HttpMethod.POST,
                        "/phone/send",
                        "/phone/verify"
                    ).permitAll()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/files/*",
                        "/images/*",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                    ).permitAll()
                    .anyRequest()
                    .authenticated()
            }
            .oauth2Login {
                oauth2 -> oauth2
                    .authorizationEndpoint {
                        authorization -> authorization
                            .authorizationRequestRepository(cookieOAuth2AuthorizationRequestRepository)
                    }
                    .userInfoEndpoint {
                        userInfo -> userInfo.userService(customOAuthUserService)
                    }
                    .successHandler(oAuth2SuccessHandler)
                    .failureHandler(oAuth2FailureHandler)
            }
            .addFilterBefore(
                jwtAuthFilter,
                UsernamePasswordAuthenticationFilter::class.java
            )

        return httpSecurity.build()
    }

    @Bean
    fun corsFilter(): CorsFilter {

        val config = CorsConfiguration().apply {

            allowCredentials = true

            corsAllowedOrigins
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .forEach { origin -> addAllowedOriginPattern(origin) }

            addAllowedHeader("*")
            addAllowedMethod("*")
        }
        val source = UrlBasedCorsConfigurationSource().apply {

            registerCorsConfiguration("/**", config)
        }

        return CorsFilter(source)
    }
}
