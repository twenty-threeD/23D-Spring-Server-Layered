package spring.springserver.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import spring.springserver.domain.oauth.service.CustomOAuthUserService;
import spring.springserver.domain.oauth.handler.OAuth2SuccessHandler;
import spring.springserver.global.handler.ApiAccessDeniedHandler;
import spring.springserver.global.handler.ApiAuthenticationEntryPoint;
import spring.springserver.global.jwt.JwtAuthFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthFilter jwtAuthFilter;
	private final ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;
	private final ApiAccessDeniedHandler apiAccessDeniedHandler;

	@Bean
	public PasswordEncoder passwordEncoder() {

		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity httpSecurity,
										   CustomOAuthUserService customOAuthUserService,
										   OAuth2SuccessHandler oAuth2SuccessHandler) throws Exception {

		httpSecurity
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.csrf(AbstractHttpConfigurer::disable)
				.cors(cors -> {})
				.exceptionHandling(exception -> exception
						.authenticationEntryPoint(apiAuthenticationEntryPoint)
						.accessDeniedHandler(apiAccessDeniedHandler)
				)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				.authorizeHttpRequests(auth
						-> auth

						.requestMatchers(
								HttpMethod.POST,
								"/api/auth/signup",
								"/api/auth/signin",
								"/api/auth/signout",
								"/api/auth/password/reset",
								"/api/member/check-username",
								"/api/member/check-email",
								"/api/member/check-phone",
								"/oauth2",
								"/login",
								"/loginSuccess"
						).permitAll()

						.requestMatchers(
								HttpMethod.POST,
								"/api/auth/password/reset/check"
						).hasRole("USER")

						.requestMatchers(
								HttpMethod.DELETE,
								"/api/delete/account"
						).hasRole("USER")

						.requestMatchers(
								"/chat-test.html",
								"/ws-stomp/**"
						).permitAll()
                               
						.requestMatchers(
								HttpMethod.POST,
								"/api/files/upload"
						).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/post"
                        ).hasRole("USER")

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/post"
                        ).hasRole("USER")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/post"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/post/*"
                        ).hasRole("USER")

						.requestMatchers(
								HttpMethod.POST,
								"/api/email/code/send",
								"/api/email/code/verify"
						).permitAll()

						.requestMatchers(
								HttpMethod.POST,
								"/api/payment/confirm",
								"/api/payment/*/cancel",
								"/api/payment/virtual-accounts"
						).hasRole("USER")

						.requestMatchers(
								HttpMethod.GET,
								"/api/payment/*",
								"/api/payment/orders/*"
						).hasRole("USER")

						.requestMatchers(
								HttpMethod.GET,
								"/api/community/post/",
								"/api/community/post/{postId}",
								"/api/community/post/search"
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
				).oauth2Login(oauth2 -> oauth2
						.userInfoEndpoint(userInfo -> userInfo
								.userService(customOAuthUserService)
						)
						.successHandler(oAuth2SuccessHandler)
				)
				.addFilterBefore(
						jwtAuthFilter,
						UsernamePasswordAuthenticationFilter.class
				);

		return httpSecurity.build();
	}

	@Bean
	public CorsFilter corsFilter() {

		CorsConfiguration config = new CorsConfiguration();
		config.setAllowCredentials(true);
		config.addAllowedOriginPattern("*");
		config.addAllowedHeader("*");
		config.addAllowedMethod("*");

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);

		return new CorsFilter(source);
	}
}
