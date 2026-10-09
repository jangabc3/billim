package com.billim.common.error;

import org.springframework.http.HttpStatus;

/**
 * 서비스 전체에서 쓰는 에러 코드. 응답의 code 필드에 이름(name)이 그대로 나간다.
 * 프론트는 메시지 글자가 아니라 이 코드로 분기한다. (메시지는 바뀔 수 있지만 코드는 바꾸지 않는다.)
 *
 * 이름 규칙: {영역}_{상황}
 * - COMMON_: 어디서나 생길 수 있는 공통 오류
 * - AUTH_/USER_: 인증·회원
 * - RESOURCE_/ITEM_: 자원 조회
 * - RESERVATION_/WAITLIST_: 예약·대기
 */
public enum ErrorCode {

    // ===== 공통 =====
    COMMON_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    COMMON_VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    COMMON_MALFORMED_BODY(HttpStatus.BAD_REQUEST, "요청 본문을 읽을 수 없습니다. 형식을 확인해주세요."),
    COMMON_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "요청 값의 형식이 올바르지 않습니다."),
    COMMON_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다. 다시 로그인해주세요."),
    COMMON_FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    COMMON_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 경로를 찾을 수 없습니다."),
    COMMON_METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식입니다."),
    COMMON_NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE, "지원하지 않는 응답 형식입니다."),
    COMMON_UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 요청 형식입니다."),
    COMMON_CONFLICT(HttpStatus.CONFLICT, "이미 처리되었거나 충돌하는 요청입니다."),
    COMMON_CONCURRENCY_FAILURE(HttpStatus.CONFLICT, "요청이 몰려 처리하지 못했습니다. 잠시 후 다시 시도해주세요."),
    COMMON_TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),
    COMMON_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),
    COMMON_EXTERNAL_API_FAILED(HttpStatus.BAD_GATEWAY, "외부 서비스와 통신 중 문제가 발생했습니다. 잠시 후 다시 시도해주세요."),
    COMMON_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE,
            "외부 서비스가 일시적으로 불안정하여 요청을 처리할 수 없습니다. 잠시 후 다시 시도해주세요."),

    // ===== 인증·회원 =====
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다."),
    AUTH_ACCOUNT_LOCKED(HttpStatus.LOCKED, "로그인 시도가 너무 많아 일시적으로 계정이 잠겼습니다. 잠시 후 다시 시도해주세요."),
    AUTH_EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),

    // ===== 자원·물품 =====
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 자원입니다."),
    ITEM_OUT_OF_STOCK(HttpStatus.CONFLICT, "재고가 없습니다. 대기 신청이 필요합니다."),

    // ===== 예약 =====
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 예약입니다."),
    RESERVATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "이 기관의 예약을 처리할 권한이 없습니다."),
    RESERVATION_ALREADY_CLOSED(HttpStatus.CONFLICT, "이미 종료된 예약은 취소할 수 없습니다."),
    RESERVATION_INVALID_STATE(HttpStatus.CONFLICT, "현재 상태에서는 처리할 수 없는 예약입니다."),
    RESERVATION_CONTENTION(HttpStatus.CONFLICT, "일시적으로 재고 경쟁이 많습니다. 잠시 후 다시 시도해주세요."),

    // ===== 대기 =====
    WAITLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 대기 신청입니다."),
    WAITLIST_ALREADY_CLOSED(HttpStatus.CONFLICT, "이미 종료된 대기 신청은 취소할 수 없습니다."),
    WAITLIST_NOT_NOTIFIED(HttpStatus.CONFLICT, "알림을 받은 대기 신청만 확정할 수 있습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    /** 응답의 code 필드에 나가는 값. */
    public String getCode() {
        return name();
    }
}