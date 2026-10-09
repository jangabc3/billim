package com.billim.service.sync;

import com.billim.adapter.seoul.SeoulReservationAdapter;
import com.billim.common.Times;
import com.billim.domain.resource.PublicResource;
import com.billim.domain.resource.ReceptionStatus;
import com.billim.domain.resource.ResourceSource;
import com.billim.domain.sync.SyncLog;
import com.billim.repository.PublicResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 서울시 공공서비스예약(종합)에서 받아온 데이터를 DB에 Upsert한다.
 * (source, externalId)로 기존 자원을 찾아, 있으면 갱신·없으면 새로 저장한다.
 *
 * GongyunuriSyncService와 달리 카테고리별로 나눠 호출할 필요가 없다 —
 * fetchAll() 하나가 서울 전역·전 카테고리 데이터를 페이지네이션까지 포함해 전부 가져온다.
 *
 * 설계 포인트
 * - 외부 API 호출(수 분 걸릴 수 있음)은 트랜잭션 밖에서 하고, DB 반영만 짧은 트랜잭션으로 묶는다.
 * - 기존 행은 출처 단위로 한 번에 읽어 Map으로 비교한다(행마다 조회하지 않는다).
 * - 이번에 받은 데이터에 없는 기존 항목은 원본에서 사라진 것으로 보고 마감(CLOSED) 처리한다.
 * 단, 받은 건수가 기존의 절반 미만이면 부분 응답일 수 있어 이 처리를 건너뛴다.
 */
@Service
public class SeoulSyncService {

    private static final Logger log = LoggerFactory.getLogger(SeoulSyncService.class);

    // 받은 건수가 기존 건수의 이 비율 미만이면 "사라진 항목 마감" 처리를 하지 않는다.
    private static final double MIN_COVERAGE_TO_CLOSE_UNSEEN = 0.5;

    private final SeoulReservationAdapter adapter;
    private final PublicResourceRepository repository;
    private final SyncLogRecorder syncLogRecorder;
    private final TransactionTemplate transactionTemplate;

    public SeoulSyncService(SeoulReservationAdapter adapter, PublicResourceRepository repository,
            SyncLogRecorder syncLogRecorder, TransactionTemplate transactionTemplate) {
        this.adapter = adapter;
        this.repository = repository;
        this.syncLogRecorder = syncLogRecorder;
        this.transactionTemplate = transactionTemplate;
    }

    public int syncAll() {
        SyncLog syncLog = SyncLog.start(ResourceSource.SEOUL_RESERVATION);
        try {
            LocalDateTime runStartedAt = Times.now();

            // 네트워크 호출은 트랜잭션 밖에서 — DB 커넥션을 오래 붙잡지 않는다.
            List<PublicResource> fetched = adapter.fetchAll();

            int[] counts = Objects.requireNonNull(
                    transactionTemplate.execute(status -> upsertAll(fetched, runStartedAt)));

            int newCount = counts[0];
            int updatedCount = counts[1];
            syncLog.succeed(newCount + updatedCount, newCount, updatedCount, 0);
            return newCount + updatedCount;
        } catch (RuntimeException e) {
            log.error("서울시 공공서비스예약 동기화 실패", e);
            syncLog.fail(e.getMessage());
            throw e;
        } finally {
            syncLogRecorder.save(syncLog);
        }
    }

    /** @return {newCount, updatedCount} */
    private int[] upsertAll(List<PublicResource> fetched, LocalDateTime runStartedAt) {
        Map<String, PublicResource> byExternalId = new HashMap<>();
        for (PublicResource existing : repository.findBySource(ResourceSource.SEOUL_RESERVATION)) {
            byExternalId.put(existing.getExternalId(), existing);
        }
        int existingBefore = byExternalId.size();

        int newCount = 0;
        int updatedCount = 0;
        for (PublicResource fresh : fetched) {
            PublicResource existing = byExternalId.get(fresh.getExternalId());
            if (existing != null) {
                existing.syncFromExternal(
                        fresh.getName(), fresh.getAddress(), fresh.getFee(),
                        fresh.getReceptionStatus(), fresh.getReceptionEndAt(),
                        fresh.getImageUrl(), fresh.getPhone(), fresh.getOperatingHours(),
                        fresh.getExternalUpdatedAt());
                updatedCount++;
            } else {
                repository.save(fresh);
                byExternalId.put(fresh.getExternalId(), fresh);
                newCount++;
            }
        }

        closeUnseenIfSafe(fetched.size(), existingBefore, runStartedAt);
        return new int[] { newCount, updatedCount };
    }

    private void closeUnseenIfSafe(int fetchedCount, int existingBefore, LocalDateTime runStartedAt) {
        if (existingBefore == 0) {
            return;
        }
        if (fetchedCount < existingBefore * MIN_COVERAGE_TO_CLOSE_UNSEEN) {
            log.warn("서울시 응답이 평소보다 너무 적어 사라진 항목 마감 처리를 건너뜁니다. 받은 건수={}, 기존 건수={}",
                    fetchedCount, existingBefore);
            return;
        }
        int closed = repository.closeUnseen(ResourceSource.SEOUL_RESERVATION, runStartedAt, ReceptionStatus.CLOSED);
        log.info("서울시 원본에서 사라진 항목 {}건을 마감 처리했습니다.", closed);
    }
}