package com.billim.repository;

import com.billim.domain.resource.PublicResource;
import com.billim.domain.resource.ResourceSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PublicResourceRepository extends JpaRepository<PublicResource, Long> {

    // 같은 자원을 재수집할 때 중복 저장 대신 갱신(Upsert)하기 위해 필요
    Optional<PublicResource> findBySourceAndExternalId(ResourceSource source, String externalId);
}