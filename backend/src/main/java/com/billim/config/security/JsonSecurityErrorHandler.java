package com.billim.config.security;

import com.billim.common.error.ApiErrorResponse;
import com.billim.common.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 스프링 시큐리티 필터 단계에서 나는 인증·권한 실패를 GlobalExceptionHandler와 같은 ApiErrorResponse
 * 형식으로 응답한다.
 * (컨트롤러 안에서 나는 예외는 GlobalExceptionHandler가 처리하고, 그 앞 단계의 실패는 여기서 처리한다.)
 *
 * - 로그인이 필요한데 안 한 경우(토큰 없음·만료·위조) → 401 COMMON_UNAUTHORIZED
 * - 로그인은 했지만 권한이 없는 경우 → 403 COMMON_FORBIDDEN
 */
@Component
public class JsonSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public JsonSecurityErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        write(request, response, ErrorCode.COMMON_UNAUTHORIZED);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        write(request, response, ErrorCode.COMMON_FORBIDDEN);
    }

    private void write(HttpServletRequest request, HttpServletResponse response, ErrorCode code)
            throws IOException {
        response.setStatus(code.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                ApiErrorResponse.of(code, code.getMessage(), request.getRequestURI()));
    }
}