package com.billim.config;

import com.billim.config.security.CustomUserDetailsService;
import com.billim.config.security.JwtAuthenticationFilter;
import com.billim.config.security.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
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
 * /api/v1/admin/**은 SYSTEM_ADMIN 역할만 접근 가능(hasRole).
 * 그 외(/api/v1/reservations, /api/v1/waitlist 등)는 로그인만 하면 접근 가능(authenticated).
 * JwtAuthenticationFilter가 UsernamePasswordAuthenticationFilter보다 먼저 실행되어
 * 토큰이 유효하면 SecurityContext에 인증 정보(역할 포함)를 미리 채워둔다.
 *
 * CORS: Expo 개발 서버(웹 미리보기, Metro 번들러)는 다른 포트(8081 등)에서 실행되므로
 * 브라우저가 기본적으로 요청을 차단한다. 개발 단계에서는 로컬 개발 서버 origin을 허용한다.
 */
@Configuration
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider, CustomUserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // REST API + JWT 조합에서는 세션 기반 CSRF 보호가 불필요
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/resources/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("SYSTEM_ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService),
                    UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 개발 단계 — Expo 웹(localhost:8081), 실기기 미리보기(사설 IP)를 모두 허용한다.
        // TODO: 실제 배포 시에는 프로덕션 프론트엔드 도메인만 명시하도록 좁혀야 한다.
        configuration.setAllowedOriginPatterns(List.of("http://localhost:*", "http://192.168.*.*:*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}