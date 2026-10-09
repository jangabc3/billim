package com.billim.api;

import com.billim.common.error.ApiErrorResponse;
import com.billim.common.error.ApiErrorResponse.ValidationError;
import com.billim.common.error.ApiException;
import com.billim.common.error.ErrorCode;
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
import java.util.List;
import java.util.Map;

/**
 * 모든 컨트롤러의 예외를 ApiErrorResponse 하나의 형식으로 통일한다.
 * { timestamp, status, code, message, path, errors? }
 *
 * 원칙
 * - 서비스 코드의 예상된 실패는 ApiException(+ErrorCode)으로 던진다. 상태 코드와 메시지는 코드가 정한다.
 * - 4xx(클라이언트 잘못)는 사용자가 이해할 수 있는 메시지를 준다.
 * - 5xx(서버 문제)는 내부 정보(스택, SQL, 클래스명 등)를 응답에 싣지 않고 서버 로그에만 남긴다.
 * - 어디에도 안 걸린 예외는 마지막 Exception 처리기가 받아 COMMON_INTERNAL_ERROR(500)로 통일한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ===== 서비스에서 던지는 예상된 실패 =====

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException e, HttpServletRequest request) {
        ErrorCode code = e.getErrorCode();
        if (code.getStatus().is5xxServerError()) {
            log.error("API 예외 {}: {} {}", code.getCode(), request.getMethod(), request.getRequestURI(), e);
            return respond(code, code.getMessage(), request);
        }
        return respond(code, e.getMessage(), request);
    }

    // ===== 400 입력 오류 =====

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException e,
            HttpServletRequest request) {
        Map<String, String> byField = new LinkedHashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            byField.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        List<ValidationError> errors = byField.entrySet().stream()
                .map(entry -> new ValidationError(entry.getKey(), entry.getValue()))
                .toList();
        return respond(ErrorCode.COMMON_VALIDATION_FAILED, null, request, errors);
    }

    /** @Validated 로 검증하는 파라미터(@RequestParam, @PathVariable)가 규칙을 어긴 경우 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException e,
            HttpServletRequest request) {
        List<ValidationError> errors = e.getConstraintViolations().stream()
                .map(v -> new ValidationError(lastNode(v.getPropertyPath().toString()), v.getMessage()))
                .toList();
        return respond(ErrorCode.COMMON_VALIDATION_FAILED, null, request, errors);
    }

    /** 요청 본문이 깨진 JSON이거나 형식(날짜, 숫자 등)이 안 맞는 경우 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleNotReadable(HttpMessageNotReadableException e,
            HttpServletRequest request) {
        return respond(ErrorCode.COMMON_MALFORMED_BODY, null, request);
    }

    /** 예: 숫자여야 하는 경로 변수에 문자가 들어온 경우 (/resources/abc) */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e,
            HttpServletRequest request) {
        List<ValidationError> errors = List.of(new ValidationError(e.getName(), "값의 형식이 올바르지 않습니다."));
        return respond(ErrorCode.COMMON_TYPE_MISMATCH, null, request, errors);
    }

    // ===== 401 / 403 =====

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationException e,
            HttpServletRequest request) {
        return respond(ErrorCode.COMMON_UNAUTHORIZED, null, request);
    }

    /** 권한이 없는 동작 → 403. 스프링 기본 문구(영어)가 노출되지 않도록 고정 메시지를 쓴다. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException e,
            HttpServletRequest request) {
        return respond(ErrorCode.COMMON_FORBIDDEN, null, request);
    }

    // ===== 409 충돌 =====

    /** 유니크 제약 위반 등 — 동시에 같은 요청이 들어왔을 때 DB가 막아준 경우 */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException e,
            HttpServletRequest request) {
        log.warn("데이터 무결성 위반: {}", e.getMostSpecificCause().getMessage());
        return respond(ErrorCode.COMMON_CONFLICT, null, request);
    }

    /** 낙관적/비관적 락 충돌, 데드락 등 — 잠시 후 다시 시도하면 되는 경우 */
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleConcurrencyFailure(ConcurrencyFailureException e,
            HttpServletRequest request) {
        log.warn("동시성 충돌: {}", e.getMessage());
        return respond(ErrorCode.COMMON_CONCURRENCY_FAILURE, null, request);
    }

    // ===== 502 / 503 외부 서비스 =====

    /**
     * 외부 공공 API(공유누리·서울시) 호출 실패 → 502.
     * 실제 원인(호출 URL, 응답 본문 등)은 서버 로그에만 남기고, 클라이언트에는 일반화된 메시지만 준다.
     */
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ApiErrorResponse> handleRestClientException(RestClientException e,
            HttpServletRequest request) {
        log.error("외부 API 호출 실패", e);
        return respond(ErrorCode.COMMON_EXTERNAL_API_FAILED, null, request);
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ApiErrorResponse> handleCircuitBreakerOpen(CallNotPermittedException e,
            HttpServletRequest request) {
        log.warn("CircuitBreaker OPEN 상태로 요청 차단: {}", e.getMessage());
        return respond(ErrorCode.COMMON_SERVICE_UNAVAILABLE, null, request);
    }

    // ===== 임시 호환 (에러-b에서 던지는 곳을 ApiException으로 모두 바꾼 뒤 제거) =====

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException e,
            HttpServletRequest request) {
        return respond(ErrorCode.COMMON_INVALID_REQUEST, e.getMessage(), request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(IllegalStateException e,
            HttpServletRequest request) {
        return respond(ErrorCode.COMMON_CONFLICT, e.getMessage(), request);
    }

    // ===== 마지막 안전망 =====

    /**
     * 위에서 처리하지 못한 모든 예외.
     * - 스프링 MVC 표준 예외(404 경로 없음, 405 메서드 불가, 415 형식 불가, 필수 파라미터 누락 등)는
     * 원래의 4xx 상태코드를 유지하고 우리 형식으로 돌려준다.
     * - 그 외는 버그로 보고 서버 로그에 스택트레이스를 남기고, 응답은 일반화된 500으로 준다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception e, HttpServletRequest request) {
        if (e instanceof ErrorResponse errorResponse) {
            HttpStatus status = HttpStatus.resolve(errorResponse.getStatusCode().value());
            if (status != null && status.is4xxClientError()) {
                ErrorCode code = codeFor(status);
                return ResponseEntity.status(status)
                        .headers(errorResponse.getHeaders())
                        .body(ApiErrorResponse.of(status, code, code.getMessage(), request.getRequestURI(), null));
            }
        }
        log.error("처리되지 않은 예외: {} {}", request.getMethod(), request.getRequestURI(), e);
        return respond(ErrorCode.COMMON_INTERNAL_ERROR, null, request);
    }

    // ===== 공통 =====

    private ErrorCode codeFor(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> ErrorCode.COMMON_NOT_FOUND;
            case METHOD_NOT_ALLOWED -> ErrorCode.COMMON_METHOD_NOT_ALLOWED;
            case NOT_ACCEPTABLE -> ErrorCode.COMMON_NOT_ACCEPTABLE;
            case UNSUPPORTED_MEDIA_TYPE -> ErrorCode.COMMON_UNSUPPORTED_MEDIA_TYPE;
            default -> ErrorCode.COMMON_INVALID_REQUEST;
        };
    }

    private String lastNode(String propertyPath) {
        int dot = propertyPath.lastIndexOf('.');
        return dot >= 0 ? propertyPath.substring(dot + 1) : propertyPath;
    }

    private ResponseEntity<ApiErrorResponse> respond(ErrorCode code, String message, HttpServletRequest request) {
        return respond(code, message, request, null);
    }

    private ResponseEntity<ApiErrorResponse> respond(ErrorCode code, String message, HttpServletRequest request,
            List<ValidationError> errors) {
        return ResponseEntity.status(code.getStatus())
                .body(ApiErrorResponse.of(code, message, request.getRequestURI(), errors));
    }
}