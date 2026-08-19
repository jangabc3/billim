package com.billim.domain.sync;

import com.billim.domain.resource.ResourceSource;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Scheduler/Spring Batch가 외부 API를 수집할 때마다 결과를 남긴다.
 * "동기화가 실패한 적이 있는가", "몇 건이 갱신됐는가"를 관리자 화면에서 보여줄 때 사용.
 */
@Entity
@Table(name = "sync_logs")
public class SyncLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResourceSource source;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private int totalCount;
    private int newCount;
    private int updatedCount;
    private int failedCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SyncStatus status;

    @Column(length = 1000)
    private String errorMessage;

    protected SyncLog() {
    }

    public static SyncLog start(ResourceSource source) {
        SyncLog log = new SyncLog();
        log.source = source;
        log.startedAt = LocalDateTime.now();
        log.status = SyncStatus.RUNNING;
        return log;
    }

    public void succeed(int total, int newCount, int updatedCount, int failedCount) {
        this.finishedAt = LocalDateTime.now();
        this.totalCount = total;
        this.newCount = newCount;
        this.updatedCount = updatedCount;
        this.failedCount = failedCount;
        this.status = failedCount == 0 ? SyncStatus.SUCCESS : SyncStatus.PARTIAL;
    }

    public void fail(String errorMessage) {
        this.finishedAt = LocalDateTime.now();
        this.status = SyncStatus.FAILED;
        this.errorMessage = errorMessage;
    }
}
