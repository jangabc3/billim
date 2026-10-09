package com.billim.common;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 서비스 전체에서 쓰는 "지금" 기준. 서울시 API의 접수 시각이 한국 시간이라
 * JVM/DB 서버 시간대(배포 환경은 보통 UTC)와 상관없이 KST로 고정해서 비교한다.
 */
public final class Times {

    public static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private Times() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(KST);
    }
}