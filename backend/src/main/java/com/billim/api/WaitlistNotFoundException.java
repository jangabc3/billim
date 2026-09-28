package com.billim.api;

public class WaitlistNotFoundException extends RuntimeException {
    public WaitlistNotFoundException(Long waitlistId) {
        super("존재하지 않는 대기 신청입니다: " + waitlistId);
    }
}