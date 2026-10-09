package com.billim.common.error;

/**
 * 서비스 코드에서 "예상된 실패"를 알릴 때 던지는 예외. 에러 코드가 상태 코드와 메시지를 결정한다.
 * 예) throw new ApiException(ErrorCode.AUTH_EMAIL_DUPLICATED);
 *
 * 서버 문제(5xx)로 분류되는 코드는 응답에 내부 사정이 새지 않도록 코드의 기본 메시지만 내보낸다.
 */
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;

    public ApiException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /** 기본 메시지 대신 상황에 맞는 메시지를 쓰고 싶을 때. */
    public ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ApiException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}