package com.billim.domain.reservation;

/**
 * 예약 대기(PENDING)는 결제·승인 절차가 생기기 전까지는 사용하지 않고,
 * MVP에서는 예약 버튼을 누르는 순간 재고가 확보되면 바로 CONFIRMED로 간다.
 *
 * CONFIRMED → RENTED → RETURNED
 *     └→ CANCELED / EXPIRED
 */
public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    RENTED,
    RETURNED,
    CANCELED,
    EXPIRED
}
