package com.billim.domain.reservation;

public enum WaitlistStatus {
    WAITING,    // 대기 중
    NOTIFIED,   // 반납 발생, 확정 대기 중 (30분 제한)
    CONVERTED,  // 예약으로 전환됨
    EXPIRED     // 30분 내 확정하지 않아 다음 대기자에게 넘어감
}
