package com.billim.api.admin;

import com.billim.service.sync.GongyunuriSyncService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 지금은 인증 없이 열려있는 임시 관리자용 엔드포인트.
 * 나중에 Spring Security 붙으면 SYSTEM_ADMIN 권한 체크를 추가해야 한다.
 * 목적: 공유누리 동기화 서비스가 실제로 DB에 데이터를 쌓는지 수동으로 눌러서 확인하기 위함.
 */
@RestController
@RequestMapping("/api/v1/admin/sync")
public class SyncAdminController {

    private final GongyunuriSyncService gongyunuriSyncService;

    public SyncAdminController(GongyunuriSyncService gongyunuriSyncService) {
        this.gongyunuriSyncService = gongyunuriSyncService;
    }

    @PostMapping("/gongyunuri")
    public String syncGongyunuri(@RequestParam String rsrcClsCd) {
        int count = gongyunuriSyncService.syncCategory(rsrcClsCd);
        return "동기화 완료: " + count + "건 처리됨";
    }
}