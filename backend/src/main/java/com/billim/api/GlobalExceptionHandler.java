package com.billim.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
        body.put("message", "입력값이 올바르지 않습니다.");
        body.put("errors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ReservationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleReservationNotFound(ReservationNotFoundException e) {
        return buildResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(WaitlistNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleWaitlistNotFound(WaitlistNotFoundException e) {
        return buildResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException e) {
        return buildResponse(HttpStatus.CONFLICT, e.getMessage());
    }

    /**
     * 외부 공공 API(공유누리·서울시) 호출 실패 → 502 Bad Gateway.
     * 실제 원인(호출 URL, 응답 본문 등)은 서버 로그에만 남기고, 클라이언트에는
     * 내부 구조를 유추할 수 있는 정보를 주지 않도록 일반화된 메시지만 반환한다.
     */
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<Map<String, Object>> handleRestClientException(RestClientException e) {
        log.error("외부 API 호출 실패", e);
        return buildResponse(HttpStatus.BAD_GATEWAY, "외부 서비스와 통신 중 문제가 발생했습니다. 잠시 후 다시 시도해주세요.");
    }

    /** 로그인 실패 누적으로 계정이 잠긴 상태 → 423 Locked */
    @ExceptionHandler(com.billim.api.auth.AccountLockedException.class)
    public ResponseEntity<Map<String, Object>> handleAccountLocked(com.billim.api.auth.AccountLockedException e) {
        return buildResponse(HttpStatus.LOCKED, e.getMessage());
    }

    @ExceptionHandler(io.github.resilience4j.circuitbreaker.CallNotPermittedException.class)
    public ResponseEntity<Map<String, Object>> handleCircuitBreakerOpen(
            io.github.resilience4j.circuitbreaker.CallNotPermittedException e) {
        log.warn("CircuitBreaker OPEN 상태로 요청 차단: {}", e.getMessage());
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE,
                "외부 서비스가 일시적으로 불안정하여 요청을 처리할 수 없습니다. 잠시 후 다시 시도해주세요.");
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message != null ? message : "요청을 처리할 수 없습니다.");
        return ResponseEntity.status(status).body(body);
    }
}