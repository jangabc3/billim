package com.billim.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 모든 에러 응답의 공통 형식. 컨트롤러 예외와 보안 필터(401/403) 응답이 모두 이 형식을 쓴다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "에러 응답")
public record ApiErrorResponse(
        @Schema(description = "에러가 발생한 시각(한국 시간)", example = "2026-10-10T02:41:12.331") String timestamp,
        @Schema(description = "HTTP 상태 코드", example = "401") int status,
        @Schema(description = "에러 코드. 프론트는 메시지가 아니라 이 값으로 분기한다.", example = "COMMON_UNAUTHORIZED") String code,
        @Schema(description = "사용자에게 보여줄 수 있는 설명", example = "인증이 필요합니다. 다시 로그인해주세요.") String message,
        @Schema(description = "요청 경로", example = "/api/v1/auth/me") String path,
        @Schema(description = "입력값 검증에 실패한 항목들. 검증 실패일 때만 포함된다.") List<ValidationError> errors) {

    @Schema(description = "입력값 검증 실패 항목")
    public record ValidationError(
            @Schema(description = "문제가 된 입력 항목", example = "email") String field,
            @Schema(description = "문제 설명", example = "올바른 이메일 형식이 아닙니다.") String message) {
    }

    public static ApiErrorResponse of(ErrorCode code, String message, String path) {
        return of(code.getStatus(), code, message, path, null);
    }

    public static ApiErrorResponse of(ErrorCode code, String message, String path, List<ValidationError> errors) {
        return of(code.getStatus(), code, message, path, errors);
    }

    /** 스프링 표준 예외처럼 상태 코드가 에러 코드와 다를 수 있을 때. */
    public static ApiErrorResponse of(HttpStatus status, ErrorCode code, String message, String path,
            List<ValidationError> errors) {
        return new ApiErrorResponse(
                com.billim.common.Times.now().truncatedTo(ChronoUnit.MILLIS).toString(),
                status.value(),
                code.getCode(),
                message != null ? message : code.getMessage(),
                path,
                errors);
    }
}