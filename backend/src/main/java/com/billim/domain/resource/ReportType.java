package com.billim.domain.resource;

public enum ReportType {
    SUSPENDED,       // 실제로는 대여가 중단됨
    HOURS_CHANGED,   // 운영 시간이 다름
    PHONE_CHANGED,   // 전화번호 변경
    CONDITION_BAD,   // 물품 상태가 좋지 않음
    NEW_LOCATION,    // 새로운 대여소 제보
    OTHER
}
