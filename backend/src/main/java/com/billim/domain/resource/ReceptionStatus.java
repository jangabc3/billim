package com.billim.domain.resource;

public enum ReceptionStatus {
    OPEN,          // 접수중
    CLOSING_SOON,  // 마감 임박
    CLOSED,        // 마감
    UNKNOWN        // API가 상태를 제공하지 않음 — "확인 필요"로 표시
}
