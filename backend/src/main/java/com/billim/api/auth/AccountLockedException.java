package com.billim.api.auth;

public class AccountLockedException extends RuntimeException {
    public AccountLockedException() {
        super("로그인 시도가 너무 많아 일시적으로 계정이 잠겼습니다. 잠시 후 다시 시도해주세요.");
    }
}