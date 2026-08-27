package com.billim.service.sync;

import com.billim.adapter.seoul.SeoulReservationAdapter;
import com.billim.domain.resource.PublicResource;
import com.billim.domain.resource.ResourceSource;
import com.billim.repository.PublicResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 서울시 공공서비스예약(종합)에서 받아온 데이터를 DB에 Upsert한다.
 * (source, externalId)로 기존 자원을 찾아, 있으면 갱신·없으면 새로 저장한다.
 *
 * GongyunuriSyncService와 달리 카테고리별로 나눠 호출할 필요가 없다 —
 * fetchAll() 하나가 서울 전역·전 카테고리 데이터를 페이지네이션까지 포함해 전부 가져온다.
 */
@Service
public class SeoulSyncService {

    private final SeoulReservationAdapter adapter;
    private final PublicResourceRepository repository;

    public SeoulSyncService(SeoulReservationAdapter adapter, PublicResourceRepository repository) {
        this.adapter = adapter;
        this.repository = repository;
    }

    @Transactional
    public int syncAll() {
        List<PublicResource> fetched = adapter.fetchAll();
        int count = 0;

        for (PublicResource fresh : fetched) {
            repository.findBySourceAndExternalId(ResourceSource.SEOUL_RESERVATION, fresh.getExternalId())
                    .ifPresentOrElse(
                            existing -> existing.syncFromExternal(
                                    fresh.getName(), fresh.getAddress(), fresh.getFee(),
                                    fresh.getReceptionStatus(), fresh.getImageUrl(), fresh.getExternalUpdatedAt()),
                            () -> repository.save(fresh));
            count++;
        }

        return count;
    }
}