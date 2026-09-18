package com.billim.service.sync;

import com.billim.adapter.gongyunuri.GongyunuriAdapter;
import com.billim.domain.resource.PublicResource;
import com.billim.domain.resource.ResourceSource;
import com.billim.domain.sync.SyncCursor;
import com.billim.repository.PublicResourceRepository;
import com.billim.repository.SyncCursorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 공유누리에서 받아온 데이터를 DB에 Upsert한다.
 * (source, externalId)로 기존 자원을 찾아, 있으면 갱신·없으면 새로 저장한다.
 *
 * 증분 수집: 전국 데이터가 많은 카테고리(예: 캠핑 17,300건)는 한 번에 다 읽으면
 * API 일일 호출 한도를 초과하므로, SyncCursor에 카테고리별로 "마지막으로 읽은 페이지"를
 * 저장해두고 실행할 때마다 그 다음부터 이어서 읽는다. 전체를 다 읽으면(reachedEnd)
 * 커서를 리셋해서 처음부터 다시 순회한다 — 새로 등록된 물품도 결국 다시 훑게 하기 위함.
 *
 * 목록 API는 이용료·세부분류를 안 줘서, 목록을 다 저장한 뒤 상세 API를 배치로 호출해
 * fee(무료/유료)와 subCategory를 덧씌운다. 상세 API가 실패해도 목록 저장 자체는
 * 이미 끝난 뒤라 전체 동기화가 죽지 않는다 — 로그만 남기고 다음 카테고리로 넘어간다.
 */
@Service
public class GongyunuriSyncService {

    private static final Logger log = LoggerFactory.getLogger(GongyunuriSyncService.class);

    private final GongyunuriAdapter adapter;
    private final PublicResourceRepository repository;
    private final SyncCursorRepository cursorRepository;

    @Value("${gongyunuri.api-key}")
    private String apiKey;

    public GongyunuriSyncService(GongyunuriAdapter adapter, PublicResourceRepository repository,
            SyncCursorRepository cursorRepository) {
        this.adapter = adapter;
        this.repository = repository;
        this.cursorRepository = cursorRepository;
    }

    @Transactional
    public int syncCategory(String rsrcClsCd) {
        SyncCursor cursor = cursorRepository.findById(rsrcClsCd)
                .orElseGet(() -> new SyncCursor(rsrcClsCd));

        GongyunuriAdapter.FetchResult result = adapter.fetchSeoulResourcesFrom(
                rsrcClsCd, apiKey, cursor.nextStartPage());

        List<PublicResource> fetched = result.content();

        int count = 0;
        for (PublicResource fresh : fetched) {
            repository.findBySourceAndExternalId(ResourceSource.SHARENURI, fresh.getExternalId())
                    .ifPresentOrElse(
                            existing -> existing.syncFromExternal(
                                    fresh.getName(), fresh.getAddress(), fresh.getFee(),
                                    fresh.getReceptionStatus(), fresh.getImageUrl(), fresh.getExternalUpdatedAt()),
                            () -> repository.save(fresh));
            count++;
        }

        enrichWithDetail(fetched);

        if (result.reachedEnd()) {
            cursor.reset();
            log.info("공유누리 카테고리 {} 전체 데이터 끝까지 읽음 — 커서를 처음으로 리셋", rsrcClsCd);
        } else {
            cursor.advance(result.lastPageRead());
        }
        cursorRepository.save(cursor);

        return count;
    }

    /**
     * 방금 목록으로 저장/갱신한 자원들의 externalId를 모아 상세 API를 배치 호출하고,
     * fee(무료/유료)와 subCategory를 채워넣는다. 실패해도 목록 동기화 결과는 그대로 유지된다.
     */
    private void enrichWithDetail(List<PublicResource> fetched) {
        List<String> rsrcNos = fetched.stream()
                .map(PublicResource::getExternalId)
                .collect(Collectors.toList());

        if (rsrcNos.isEmpty())
            return;

        Map<String, GongyunuriAdapter.DetailInfo> details;
        try {
            details = adapter.fetchDetailBatch(rsrcNos, apiKey);
        } catch (Exception e) {
            log.warn("공유누리 상세 API 조회 실패 — 목록 데이터는 그대로 저장됨. 대상 {}건, 사유: {}",
                    rsrcNos.size(), e.getMessage());
            return;
        }

        for (String rsrcNo : rsrcNos) {
            GongyunuriAdapter.DetailInfo detail = details.get(rsrcNo);
            if (detail == null)
                continue;

            repository.findBySourceAndExternalId(ResourceSource.SHARENURI, rsrcNo)
                    .ifPresent(resource -> resource.applyGongyunuriDetail(detail.fee(), detail.subCategory()));
        }
    }
}