package com.billim.api.reservation;

import com.billim.common.error.ApiException;
import com.billim.common.error.ErrorCode;

/** 존재하지 않거나 본인 소유가 아닌 대기 신청에 접근했을 때. (404) */
public class WaitlistNotFoundException extends ApiException {
    public WaitlistNotFoundException(Long waitlistId) {
        super(ErrorCode.WAITLIST_NOT_FOUND);
    }
}