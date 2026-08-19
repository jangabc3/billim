package com.billim.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 지금은 전체 API를 열어둔 상태(permitAll).
 * 다음 단계에서 할 일:
 *   1. JwtAuthenticationFilter 추가 후 SecurityFilterChain에 addFilterBefore로 연결
 *   2. /api/v1/resources 같은 조회 API는 permitAll 유지
 *   3. /api/v1/reservations, /api/v1/admin/** 는 인증 필요하도록 authorizeHttpRequests 세분화
 *   4. UserDetailsService 구현체를 만들어서 User 엔티티와 연결
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // REST API + JWT 조합에서는 세션 기반 CSRF 보호가 불필요
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll() // TODO: JWT 붙이면 여기 세분화
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
