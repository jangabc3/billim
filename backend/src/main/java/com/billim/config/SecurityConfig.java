package com.billim.config;

import com.billim.config.security.CustomUserDetailsService;
import com.billim.config.security.JsonSecurityErrorHandler;
import com.billim.config.security.JwtAuthenticationFilter;
import com.billim.config.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * /api/v1/resources, /api/v1/auth는 로그인 없이 접근 가능(permitAll).
 * 단, /api/v1/auth/me는 예외로 인증이 필요함(authenticated) — 로그인한 사용자의 프로필 조회용.
 * /api/v1/admin/**은 SYSTEM_ADMIN 역할만 접근 가능(hasRole).
 * /actuator/health/**는 로드밸런서·오케스트레이터의 헬스체크용으로 인증 없이 허용한다.
 * (application.yml에서 health 외의 actuator 엔드포인트는 노출하지 않는다.)
 * 그 외(/api/v1/reservations, /api/v1/waitlist 등)는 로그인만 하면 접근 가능(authenticated).
 * JwtAuthenticationFilter가 UsernamePasswordAuthenticationFilter보다 먼저 실행되어
 * 토큰이 유효하면 SecurityContext에 인증 정보(역할 포함)를 미리 채워둔다.
 *
 * 인증이 필요한데 안 된 요청은 401, 권한이 없는 요청은 403을 JSON으로 응답한다(JsonSecurityErrorHandler).
 * 인증은 매 요청의 JWT 헤더로만 하므로 서버 세션은 만들지 않는다(STATELESS).
 *
 * CORS: 브라우저(Expo 웹 등)는 다른 origin의 API 요청을 기본적으로 차단한다.
 * 허용 origin은 설정값(billim.cors.allowed-origin-patterns)으로 받는다.
 * 로컬은 application.yml 기본값(localhost, 사설 IP), 운영은 환경변수 CORS_ALLOWED_ORIGINS로 실제
 * 도메인만 지정한다.
 * 토큰은 쿠키가 아니라 Authorization 헤더로 보내므로 자격 증명(쿠키) 허용 설정은 켜지 않는다.
 * (네이티브 앱은 Origin 헤더를 보내지 않아 CORS 대상이 아니다.)
 */
@Configuration
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final JsonSecurityErrorHandler securityErrorHandler;
    private final List<String> allowedOriginPatterns;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider,
            CustomUserDetailsService userDetailsService,
            JsonSecurityErrorHandler securityErrorHandler,
            @Value("${billim.cors.allowed-origin-patterns}") List<String> allowedOriginPatterns) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
        this.securityErrorHandler = securityErrorHandler;
        this.allowedOriginPatterns = allowedOriginPatterns;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // REST API + JWT 조합에서는 세션 기반 CSRF 보호가 불필요
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll() // 내부 에러 포워딩이 인증 컨텍스트 없이도 403 대신 실제 에러를 보여주게 함
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll() // 헬스체크
                        .requestMatchers("/api/v1/auth/me").authenticated() // permitAll보다 먼저: 더 구체적인 규칙이 우선
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/resources/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll() // 운영(prod)에서는 springdoc 자체를
                                                                                          // 꺼서 404가 된다
                        .requestMatchers("/api/v1/admin/sync/seoul").hasAuthority("ROLE_SYSTEM_ADMIN")
                        .requestMatchers("/api/v1/admin/sync/gongyunuri").hasAuthority("ROLE_SYSTEM_ADMIN")
                        .requestMatchers("/api/v1/admin/**").hasAuthority("ROLE_SYSTEM_ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(allowedOriginPatterns);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        // 쿠키를 쓰지 않으므로 allowCredentials는 설정하지 않는다(기본 false).

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}