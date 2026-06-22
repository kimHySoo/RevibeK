package com.ssafy.revibek.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.ssafy.revibek.auth.ApiAuthenticationEntryPoint;
import com.ssafy.revibek.auth.JwtAuthenticationFilter;
import com.ssafy.revibek.auth.OAuth2FailureHandler;
import com.ssafy.revibek.auth.OAuth2SuccessHandler;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;
    private final ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;

    @Value("${app.oauth.google.enabled:false}")
    private boolean googleOAuthEnabled;

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret:}")
    private String googleClientSecret;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // OAuth2 authorization request 저장을 위해 최소한의 세션 생성을 허용한다.
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            // /api/** 미인증 요청은 oauth2Login()의 기본 동작(구글로 302 리다이렉트) 대신 401 JSON을
            // 받도록 한다. 그대로 두면 axios가 accounts.google.com까지 리다이렉트를 따라가다 CORS 에러로
            // 막혀서 "로그인 필요"라는 진짜 원인이 가려진다 (docs/error/error04.md 참고).
            .exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor(
                apiAuthenticationEntryPoint,
                PathPatternRequestMatcher.withDefaults().matcher("/api/**")
            ))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/**",
                    "/oauth2/**",
                    "/login/oauth2/**",
                    "/auth/google/callback",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/songs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/reviews/songs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/challenges").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/challenges/templates").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/challenges/{challengeId}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/radio/public").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/radio/public/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/radio/*/likes/count").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/users/me").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/users/me").authenticated()
                .requestMatchers("/api/usersongs/**").authenticated()
                .requestMatchers("/api/radio/**").authenticated()
                .requestMatchers("/api/likes/**").authenticated()
                .requestMatchers("/api/playlists/**").authenticated()
                .requestMatchers("/api/reviews/**").authenticated()
                .requestMatchers("/api/follows/**").authenticated()
                .requestMatchers("/api/plans/**").authenticated()
                .requestMatchers("/api/challenges/**").authenticated()
                .requestMatchers("/api/coaching/**").authenticated()
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        if (isGoogleOAuthReady()) {
            http.oauth2Login(oauth2 -> oauth2
                .redirectionEndpoint(redirection -> redirection.baseUri("/auth/google/callback"))
                .successHandler(oAuth2SuccessHandler)
                .failureHandler(oAuth2FailureHandler)
            );
        }

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(parseAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private boolean isGoogleOAuthReady() {
        return googleOAuthEnabled
            && StringUtils.hasText(googleClientId)
            && StringUtils.hasText(googleClientSecret);
    }

    private List<String> parseAllowedOrigins() {
        return Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .toList();
    }
}