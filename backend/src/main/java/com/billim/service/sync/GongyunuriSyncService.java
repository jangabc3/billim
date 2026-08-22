package com.billim.service.sync;

import com.billim.adapter.gongyunuri.GongyunuriAdapter;
import com.billim.domain.resource.PublicResource;
import com.billim.domain.resource.ResourceSource;
import com.billim.repository.PublicResourceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 공유누리에서 받아온 데이터를 DB에 Upsert한다.
 * (source, externalId)로 기존 자원을 찾아, 있으면 갱신·없으면 새로 저장한다.
 */
@Service
public class GongyunuriSyncService {

    private final GongyunuriAdapter adapter;
    private final PublicResourceRepository repository;

    @Value("${gongyunuri.api-key}")
    private String apiKey;

    public GongyunuriSyncService(GongyunuriAdapter adapter, PublicResourceRepository repository) {
        this.adapter = adapter;
        this.repository = repository;
    }

    @Transactional
    public int syncCategory(String rsrcClsCd) {
        List<PublicResource> fetched = adapter.fetchAllSeoulResources(rsrcClsCd, apiKey);

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
        return count;
    }
}