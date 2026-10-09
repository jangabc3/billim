package com.billim.service.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * 공유누리·서울시 공공서비스예약 데이터를 매일 새벽 자동으로 재동기화한다.
 *
 * 각 작업은 서로 격리해서 실행한다 — 공유누리 카테고리 하나가 실패(서킷브레이커 오픈 등)해도
 * 나머지 카테고리와 서울시 동기화는 계속 진행되어야 하기 때문이다.
 */
@Component
public class SyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(SyncScheduler.class);

    private static final String[] GONGYUNURI_CATEGORY_CODES = {
            "010000", "010100", "010200", "010500", "010700", "020000", "030000", "040000"
    };

    private final GongyunuriSyncService gongyunuriSyncService;
    private final SeoulSyncService seoulSyncService;

    public SyncScheduler(GongyunuriSyncService gongyunuriSyncService, SeoulSyncService seoulSyncService) {
        this.gongyunuriSyncService = gongyunuriSyncService;
        this.seoulSyncService = seoulSyncService;
    }

    /** 매일 새벽 3시(한국 시간) 실행. 서버가 UTC로 돌아도 같은 시각에 실행되도록 시간대를 명시한다. */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void syncAll() {
        // 서울시를 먼저: 접수 마감일을 주는 출처이고 매번 전체를 받아오기 때문에 가장 중요하다.
        runIsolated("서울시 공공서비스예약", seoulSyncService::syncAll);

        for (String categoryCode : GONGYUNURI_CATEGORY_CODES) {
            runIsolated("공유누리 " + categoryCode, () -> gongyunuriSyncService.syncCategory(categoryCode));
        }
    }

    private void runIsolated(String label, Supplier<Integer> task) {
        try {
            int count = task.get();
            log.info("{} 동기화 완료: {}건", label, count);
        } catch (RuntimeException e) {
            log.error("{} 동기화 실패 — 다음 작업을 계속 진행합니다", label, e);
        }
    }
}