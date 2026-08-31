package com.billim.api;

/** 예약 생성 요청. userId는 로그인 기능이 붙기 전까지 임시로 직접 받는다. */
public record ReservationCreateRequest(Long userId, Long rentalItemId) {
}