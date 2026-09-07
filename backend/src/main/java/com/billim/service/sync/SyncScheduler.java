package com.billim.service.sync;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 공유누리·서울시 공공서비스예약 데이터를 매일 새벽 자동으로 재동기화한다.
 */
@Component
public class SyncScheduler {

    private static final String[] GONGYUNURI_CATEGORY_CODES = {
            "010000", "010100", "010200", "010500", "010700", "020000", "030000", "040000"
    };

    private final GongyunuriSyncService gongyunuriSyncService;
    private final SeoulSyncService seoulSyncService;

    public SyncScheduler(GongyunuriSyncService gongyunuriSyncService, SeoulSyncService seoulSyncService) {
        this.gongyunuriSyncService = gongyunuriSyncService;
        this.seoulSyncService = seoulSyncService;
    }

    /** 매일 새벽 3시 실행. */
    @Scheduled(cron = "0 0 3 * * *")
    public void syncAll() {
        for (String categoryCode : GONGYUNURI_CATEGORY_CODES) {
            gongyunuriSyncService.syncCategory(categoryCode);
        }
        seoulSyncService.syncAll();
    }
}