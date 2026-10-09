package com.billim.service.sync;

import com.billim.adapter.gongyunuri.GongyunuriAdapter;
import com.billim.domain.resource.PublicResource;
import com.billim.domain.resource.ResourceSource;
import com.billim.domain.sync.SyncCursor;
import com.billim.domain.sync.SyncLog;
import com.billim.repository.PublicResourceRepository;
import com.billim.repository.SyncCursorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 공유누리에서 받아온 데이터를 DB에 Upsert한다.
 * (source, externalId)로 기존 자원을 찾아, 있으면 갱신·없으면 새로 저장한다.
 *
 * 증분 수집: 전국 데이터가 많은 카테고리(예: 캠핑 17,300건)는 한 번에 다 읽으면
 * API 일일 호출 한도를 초과하므로, SyncCursor에 카테고리별로 "마지막으로 읽은 페이지"를
 * 저장해두고 실행할 때마다 그 다음부터 이어서 읽는다. 전체를 다 읽으면(reachedEnd)
 * 커서를 리셋해서 처음부터 다시 순회한다 — 새로 등록된 물품도 결국 다시 훑게 하기 위함.
 * 이런 증분 방식이라 "이번에 안 받은 행 = 사라진 행"으로 볼 수 없어서, 서울시와 달리
 * 사라진 항목 마감 처리는 하지 않는다.
 *
 * 목록 API는 이용료·세부분류를 안 줘서, 목록 수집 뒤 상세 API를 배치로 호출해
 * fee(무료/유료)와 subCategory를 덧씌운다. 상세 API가 실패해도 목록 저장 자체는
 * 진행된다 — 로그만 남기고 계속한다.
 *
 * 설계 포인트
 * - 외부 API 호출(목록 최대 40페이지 + 상세 배치)은 트랜잭션 밖에서 하고, DB 반영과 커서 갱신만
 * 하나의 짧은 트랜잭션으로 묶는다. (반영이 실패하면 커서도 전진하지 않는다.)
 * - 기존 행은 이번에 받은 externalId들만 묶어서 한 번에 읽는다(행마다 조회하지 않는다).
 *
 * 카테고리 한 번 호출할 때마다 SyncLog를 하나 남긴다 — 스케줄러가 매일 새벽 8개
 * 카테고리를 돌면서 8건, 관리자가 수동으로 트리거해도 그때마다 1건씩 쌓인다.
 */
@Service
public class GongyunuriSyncService {

    private static final Logger log = LoggerFactory.getLogger(GongyunuriSyncService.class);

    // IN 조건에 한 번에 넣을 externalId 개수
    private static final int LOOKUP_CHUNK_SIZE = 500;

    private final GongyunuriAdapter adapter;
    private final PublicResourceRepository repository;
    private final SyncCursorRepository cursorRepository;
    private final SyncLogRecorder syncLogRecorder;
    private final TransactionTemplate transactionTemplate;

    @Value("${gongyunuri.api-key}")
    private String apiKey;

    public GongyunuriSyncService(GongyunuriAdapter adapter, PublicResourceRepository repository,
            SyncCursorRepository cursorRepository, SyncLogRecorder syncLogRecorder,
            TransactionTemplate transactionTemplate) {
        this.adapter = adapter;
        this.repository = repository;
        this.cursorRepository = cursorRepository;
        this.syncLogRecorder = syncLogRecorder;
        this.transactionTemplate = transactionTemplate;
    }

    public int syncCategory(String rsrcClsCd) {
        SyncLog syncLog = SyncLog.start(ResourceSource.SHARENURI);
        try {
            int[] counts = doSync(rsrcClsCd);
            int newCount = counts[0];
            int updatedCount = counts[1];
            syncLog.succeed(newCount + updatedCount, newCount, updatedCount, 0);
            return newCount + updatedCount;
        } catch (RuntimeException e) {
            log.error("공유누리 카테고리 {} 동기화 실패", rsrcClsCd, e);
            syncLog.fail(e.getMessage());
            throw e;
        } finally {
            syncLogRecorder.save(syncLog);
        }
    }

    /** @return {newCount, updatedCount} */
    private int[] doSync(String rsrcClsCd) {
        int startPage = cursorRepository.findById(rsrcClsCd)
                .map(SyncCursor::nextStartPage)
                .orElseGet(() -> new SyncCursor(rsrcClsCd).nextStartPage());

        // 네트워크 호출은 모두 트랜잭션 밖에서
        GongyunuriAdapter.FetchResult result = adapter.fetchSeoulResourcesFrom(rsrcClsCd, apiKey, startPage);
        List<PublicResource> fetched = result.content();
        Map<String, GongyunuriAdapter.DetailInfo> details = fetchDetailsSafely(fetched);

        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            int[] counts = upsert(fetched, details);
            updateCursor(rsrcClsCd, result);
            return counts;
        }));
    }

    private int[] upsert(List<PublicResource> fetched, Map<String, GongyunuriAdapter.DetailInfo> details) {
        Map<String, PublicResource> byExternalId = loadExisting(fetched);

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

        // 상세 API 결과(이용료·세부분류)를 방금 저장/갱신한 행에 덧씌운다.
        for (Map.Entry<String, GongyunuriAdapter.DetailInfo> entry : details.entrySet()) {
            PublicResource resource = byExternalId.get(entry.getKey());
            if (resource != null) {
                resource.applyGongyunuriDetail(entry.getValue().fee(), entry.getValue().subCategory());
            }
        }

        return new int[] { newCount, updatedCount };
    }

    /** 이번에 받은 externalId에 해당하는 기존 행을 묶음(chunk)으로 나눠 조회한다. */
    private Map<String, PublicResource> loadExisting(List<PublicResource> fetched) {
        List<String> ids = fetched.stream()
                .map(PublicResource::getExternalId)
                .distinct()
                .collect(Collectors.toList());

        Map<String, PublicResource> byExternalId = new HashMap<>();
        for (int i = 0; i < ids.size(); i += LOOKUP_CHUNK_SIZE) {
            List<String> chunk = ids.subList(i, Math.min(i + LOOKUP_CHUNK_SIZE, ids.size()));
            for (PublicResource existing : repository.findBySourceAndExternalIdIn(ResourceSource.SHARENURI, chunk)) {
                byExternalId.put(existing.getExternalId(), existing);
            }
        }
        return byExternalId;
    }

    private void updateCursor(String rsrcClsCd, GongyunuriAdapter.FetchResult result) {
        SyncCursor cursor = cursorRepository.findById(rsrcClsCd)
                .orElseGet(() -> new SyncCursor(rsrcClsCd));

        if (result.reachedEnd()) {
            cursor.reset();
            log.info("공유누리 카테고리 {} 전체 데이터 끝까지 읽음 — 커서를 처음으로 리셋", rsrcClsCd);
        } else {
            cursor.advance(result.lastPageRead());
        }
        cursorRepository.save(cursor);
    }

    /**
     * 방금 받은 자원들의 externalId로 상세 API를 배치 호출해 fee/subCategory 정보를 가져온다.
     * 실패해도 목록 동기화는 계속 진행해야 하므로 예외는 로그만 남기고 빈 결과를 돌려준다.
     */
    private Map<String, GongyunuriAdapter.DetailInfo> fetchDetailsSafely(List<PublicResource> fetched) {
        List<String> rsrcNos = fetched.stream()
                .map(PublicResource::getExternalId)
                .collect(Collectors.toList());

        if (rsrcNos.isEmpty()) {
            return Map.of();
        }

        try {
            return adapter.fetchDetailBatch(rsrcNos, apiKey);
        } catch (Exception e) {
            log.warn("공유누리 상세 API 조회 실패 — 목록 데이터는 그대로 저장됨. 대상 {}건, 사유: {}",
                    rsrcNos.size(), e.getMessage());
            return Map.of();
        }
    }
}