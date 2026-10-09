package com.billim.config.security;

import com.billim.common.Times;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 스프링 시큐리티 필터 단계에서 나는 인증·권한 실패를 GlobalExceptionHandler와 같은 JSON 형식으로 응답한다.
 * (컨트롤러 안에서 나는 예외는 GlobalExceptionHandler가 처리하고, 그 앞 단계의 실패는 여기서 처리한다.)
 *
 * - 로그인이 필요한데 안 한 경우(토큰 없음·만료·위조) → 401
 * - 로그인은 했지만 권한이 없는 경우 → 403
 */
@Component
public class JsonSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, "인증이 필요합니다. 다시 로그인해주세요.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        write(response, HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
    }

    // 메시지는 고정 문자열이라 따옴표 이스케이프가 필요 없다.
    private void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}",
                Times.now(), status.value(), status.getReasonPhrase(), message));
    }
}