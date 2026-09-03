package com.billim.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 컨트롤러에서 던진 예외를 적절한 HTTP 상태코드와 일관된 응답 형태로 변환한다.
 * 이게 없으면 IllegalArgumentException 같은 "입력값이 잘못됐다"는 의미의 예외도
 * 스프링 기본 처리로 500(서버 에러)이 되어버려, 클라이언트가 "내 잘못인지 서버 잘못인지"
 * 구분할 수 없게 된다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 잘못된 입력값 (존재하지 않는 리소스 참조, 중복 이메일 등) → 400 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /** 도메인 규칙 위반 (재고 없음, 잘못된 상태 전이 등) → 409 Conflict */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException e) {
        return buildResponse(HttpStatus.CONFLICT, e.getMessage());
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