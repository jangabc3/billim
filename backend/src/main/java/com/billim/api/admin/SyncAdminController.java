package com.billim.api.admin;

import com.billim.service.sync.GongyunuriSyncService;
import com.billim.service.sync.SeoulSyncService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자용 동기화 트리거 엔드포인트.
 * SecurityConfig에서 /api/v1/admin/**은 SYSTEM_ADMIN 역할만 접근 가능하도록 막혀 있다.
 * 목적: 각 동기화 서비스가 실제로 DB에 데이터를 쌓는지 수동으로 눌러서 확인하기 위함.
 */
@RestController
@RequestMapping("/api/v1/admin/sync")
public class SyncAdminController {

    private final GongyunuriSyncService gongyunuriSyncService;
    private final SeoulSyncService seoulSyncService;

    public SyncAdminController(GongyunuriSyncService gongyunuriSyncService,
            SeoulSyncService seoulSyncService) {
        this.gongyunuriSyncService = gongyunuriSyncService;
        this.seoulSyncService = seoulSyncService;
    }

    @PostMapping("/gongyunuri")
    public String syncGongyunuri(@RequestParam String rsrcClsCd) {
        int count = gongyunuriSyncService.syncCategory(rsrcClsCd);
        return "동기화 완료: " + count + "건 처리됨";
    }

    @PostMapping("/seoul")
    public String syncSeoul() {
        int count = seoulSyncService.syncAll();
        return "동기화 완료: " + count + "건 처리됨";
    }
}