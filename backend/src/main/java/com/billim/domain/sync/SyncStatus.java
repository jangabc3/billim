package com.billim.domain.sync;

public enum SyncStatus {
    RUNNING,
    SUCCESS,
    PARTIAL,  // 일부 건은 실패했지만 전체는 완료됨
    FAILED
}
