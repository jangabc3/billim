package com.billim.domain.item;

/**
 * 물품 대여 카테고리(공구~체육)와 서울시 공공서비스예약 카테고리(시설대관·교육·문화행사·진료)를
 * 하나의 enum으로 통합했다. 실제 API 테스트 결과(강남구 체육시설 응답) 기준으로
 * MAXCLASSNM에 "시설대관/교육/문화행사/진료"가 나오는 걸 확인하고 추가했다.
 *
 * MEDICAL(의료용품 대여, 예: 휠체어)과 CLINIC(진료 예약)은 의미가 달라 분리했다.
 */
public enum Category {
    TOOL,       // 공구
    SUIT,       // 정장
    BABY,       // 유아
    MEDICAL,    // 의료용품 (휠체어, 보행 보조기 등 — 빌림/공유누리 물품)
    CAMPING,    // 캠핑
    SPORTS,     // 체육시설 (서울시 공공서비스예약)
    FACILITY,   // 시설대관
    EDUCATION,  // 교육강좌
    CULTURE,    // 문화행사
    CLINIC      // 진료 예약
}
