package com.billim.domain.user;

public enum UserRole {
    USER,               // 물품 조회·예약·취소
    INSTITUTION_ADMIN,  // 담당 기관의 물품·재고·대여 관리
    SYSTEM_ADMIN        // 기관 등록·CSV 가져오기·전체 관리
}
