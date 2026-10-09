package com.billim.api;

import com.billim.api.auth.AccountLockedException;
import com.billim.api.reservation.ReservationNotFoundException;
import com.billim.api.reservation.WaitlistNotFoundException;
import com.billim.common.Times;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 모든 컨트롤러의 예외를 하나의 JSON 형식으로 통일한다.
 * { timestamp, status, error, message } (+ 입력 검증 실패 시 errors)
 *
 * 원칙
 * - 4xx(클라이언트 잘못)는 사용자가 이해할 수 있는 메시지를 준다.
 * - 5xx(서버 문제)는 내부 정보(스택, SQL, 클래스명 등)를 응답에 싣지 않고 서버 로그에만 남긴다.
 * - 어디에도 안 걸린 예외는 마지막 Exception 처리기가 받아 500으로 통일한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ===== 400 Bad Request =====

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        Map<String, Object> body = body(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다.");
        body.put("errors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /** @Validated 로 검증하는 파라미터(@RequestParam, @PathVariable)가 규칙을 어긴 경우 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다.");
    }

    /** 요청 본문이 깨진 JSON이거나 형식(날짜, 숫자 등)이 안 맞는 경우 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleNotReadable(HttpMessageNotReadableException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, "요청 본문을 읽을 수 없습니다. 형식을 확인해주세요.");
    }

    /** 예: 숫자여야 하는 경로 변수에 문자가 들어온 경우 (/resources/abc) */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, "요청 값의 형식이 올바르지 않습니다: " + e.getName());
    }

    // ===== 401 / 403 =====

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException e) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "인증이 필요합니다. 다시 로그인해주세요.");
    }

    /** 기관 관리 권한이 없는 사용자가 다른 기관의 예약을 처리하려 할 때 → 403 Forbidden */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException e) {
        return buildResponse(HttpStatus.FORBIDDEN, e.getMessage());
    }

    // ===== 404 =====

    @ExceptionHandler(ReservationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleReservationNotFound(ReservationNotFoundException e) {
        return buildResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(WaitlistNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleWaitlistNotFound(WaitlistNotFoundException e) {
        return buildResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // ===== 409 Conflict =====

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException e) {
        return buildResponse(HttpStatus.CONFLICT, e.getMessage());
    }

    /** 유니크 제약 위반 등 — 동시에 같은 요청이 들어왔을 때 DB가 막아준 경우 */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("데이터 무결성 위반: {}", e.getMostSpecificCause().getMessage());
        return buildResponse(HttpStatus.CONFLICT, "이미 처리되었거나 충돌하는 요청입니다.");
    }

    /** 낙관적/비관적 락 충돌, 데드락 등 — 잠시 후 다시 시도하면 되는 경우 */
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<Map<String, Object>> handleConcurrencyFailure(ConcurrencyFailureException e) {
        log.warn("동시성 충돌: {}", e.getMessage());
        return buildResponse(HttpStatus.CONFLICT, "요청이 몰려 처리하지 못했습니다. 잠시 후 다시 시도해주세요.");
    }

    // ===== 423 =====

    /** 로그인 실패 누적으로 계정이 잠긴 상태 → 423 Locked */
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<Map<String, Object>> handleAccountLocked(AccountLockedException e) {
        return buildResponse(HttpStatus.LOCKED, e.getMessage());
    }

    // ===== 502 / 503 =====

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

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<Map<String, Object>> handleCircuitBreakerOpen(CallNotPermittedException e) {
        log.warn("CircuitBreaker OPEN 상태로 요청 차단: {}", e.getMessage());
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE,
                "외부 서비스가 일시적으로 불안정하여 요청을 처리할 수 없습니다. 잠시 후 다시 시도해주세요.");
    }

    // ===== 마지막 안전망 =====

    /**
     * 위에서 처리하지 못한 모든 예외.
     * - 스프링 MVC 표준 예외(404 경로 없음, 405 메서드 불가, 415 형식 불가, 필수 파라미터 누락 등)는
     * 원래의 4xx 상태코드를 유지하고 우리 JSON 형식으로 돌려준다.
     * - 그 외는 버그로 보고 서버 로그에 스택트레이스를 남기고, 응답은 일반화된 500으로 준다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception e, HttpServletRequest request) {
        if (e instanceof ErrorResponse errorResponse) {
            HttpStatus status = HttpStatus.resolve(errorResponse.getStatusCode().value());
            if (status != null && status.is4xxClientError()) {
                return ResponseEntity.status(status)
                        .headers(errorResponse.getHeaders())
                        .body(body(status, clientMessage(status)));
            }
        }
        log.error("처리되지 않은 예외: {} {}", request.getMethod(), request.getRequestURI(), e);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
    }

    // ===== 공통 =====

    private String clientMessage(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "잘못된 요청입니다.";
            case NOT_FOUND -> "요청한 경로를 찾을 수 없습니다.";
            case METHOD_NOT_ALLOWED -> "지원하지 않는 요청 방식입니다.";
            case NOT_ACCEPTABLE, UNSUPPORTED_MEDIA_TYPE -> "지원하지 않는 요청 형식입니다.";
            default -> "요청을 처리할 수 없습니다.";
        };
    }

    private Map<String, Object> body(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Times.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message != null ? message : "요청을 처리할 수 없습니다.");
        return body;
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(body(status, message));
    }
}