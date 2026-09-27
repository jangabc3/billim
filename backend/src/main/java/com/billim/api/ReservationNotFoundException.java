package com.billim.api;

/** 존재하지 않거나 본인 소유가 아닌 예약에 접근했을 때 던진다 — 둘 다 404로 응답해 구분하지 않는다. */
public class ReservationNotFoundException extends RuntimeException {
    public ReservationNotFoundException(Long reservationId) {
        super("존재하지 않는 예약입니다: " + reservationId);
    }
}