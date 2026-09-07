package com.billim.service.reservation;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 대기열 확정 기한(30분)이 지난 항목을 주기적으로 만료 처리한다.
 * 5분마다 실행.
 */
@Component
public class WaitlistScheduler {

    private final WaitlistService waitlistService;

    public WaitlistScheduler(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    @Scheduled(fixedRate = 5 * 60 * 1000) // 5분마다
    public void expireOverdueWaitlistNotifications() {
        waitlistService.expireOverdueNotifications();
    }
}