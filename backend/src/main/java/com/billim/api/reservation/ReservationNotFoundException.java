package com.billim.api.reservation;

import com.billim.common.error.ApiException;
import com.billim.common.error.ErrorCode;

/** 존재하지 않거나 본인 소유가 아닌 예약에 접근했을 때 던진다 — 둘 다 404로 응답해 구분하지 않는다. */
public class ReservationNotFoundException extends ApiException {
    public ReservationNotFoundException(Long reservationId) {
        super(ErrorCode.RESERVATION_NOT_FOUND);
    }
}