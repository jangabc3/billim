package com.billim.repository;

import com.billim.domain.resource.PublicResource;
import com.billim.domain.resource.ReceptionStatus;
import com.billim.domain.resource.ResourceSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PublicResourceRepository
                extends JpaRepository<PublicResource, Long>, PublicResourceRepositoryCustom {

        // 같은 자원을 재수집할 때 중복 저장 대신 갱신(Upsert)하기 위해 필요
        Optional<PublicResource> findBySourceAndExternalId(ResourceSource source, String externalId);

        /** 동기화 시 출처별 기존 행을 한 번에 읽어 행마다 조회(N+1)하지 않기 위함. */
        List<PublicResource> findBySource(ResourceSource source);

        /** 동기화 시 이번에 받은 externalId들에 해당하는 기존 행만 한 번에 읽는다. (호출부에서 적당한 크기로 나눠 호출) */
        List<PublicResource> findBySourceAndExternalIdIn(ResourceSource source, Collection<String> externalIds);

        /**
         * 이번 동기화에서 한 번도 갱신되지 않은(= 원본 API에서 사라진) 항목을 마감 처리한다.
         * lastSyncedAt이 동기화 시작 시각보다 오래된 행이 대상이며, 이미 마감된 행은 건드리지 않는다.
         * 같은 트랜잭션에서 수정한 엔티티가 있으므로 실행 전에 flush하고, 실행 후 영속성 컨텍스트를 비운다.
         */
        @Modifying(flushAutomatically = true, clearAutomatically = true)
        @Query("update PublicResource r set r.receptionStatus = :closed " +
                        "where r.source = :source and r.lastSyncedAt < :threshold " +
                        "and (r.receptionStatus is null or r.receptionStatus <> :closed)")
        int closeUnseen(@Param("source") ResourceSource source,
                        @Param("threshold") LocalDateTime threshold,
                        @Param("closed") ReceptionStatus closed);

        /**
         * 중심좌표(lat, lng)에서 반경(radiusMeters, 단위: 미터) 이내의 자원을 찾는다.
         * PostGIS의 ST_DWithin은 QueryDSL 표준 지원이 미흡해 네이티브 쿼리로 처리한다.
         * geography 타입 좌표 비교라 별도 형변환 없이 미터 단위 반경이 그대로 적용된다.
         */
        @Query(value = "SELECT * FROM public_resources r " +
                        "WHERE ST_DWithin(r.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusMeters)", countQuery = "SELECT COUNT(*) FROM public_resources r "
                                        +
                                        "WHERE ST_DWithin(r.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusMeters)", nativeQuery = true)
        Page<PublicResource> findNearby(@Param("lat") double lat, @Param("lng") double lng,
                        @Param("radiusMeters") double radiusMeters, Pageable pageable);
}