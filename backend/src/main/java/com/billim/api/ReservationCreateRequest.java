package com.billim.api;

/** 예약 생성 요청. userId는 이제 요청 바디가 아니라 JWT 인증 정보에서 가져온다. */
public record ReservationCreateRequest(Long rentalItemId) {
}