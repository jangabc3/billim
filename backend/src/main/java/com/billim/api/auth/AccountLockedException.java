package com.billim.api.auth;

import com.billim.common.error.ApiException;
import com.billim.common.error.ErrorCode;

/** 로그인 실패가 누적되어 계정이 잠긴 상태에서 로그인을 시도했을 때. (423) */
public class AccountLockedException extends ApiException {
    public AccountLockedException() {
        super(ErrorCode.AUTH_ACCOUNT_LOCKED);
    }
}