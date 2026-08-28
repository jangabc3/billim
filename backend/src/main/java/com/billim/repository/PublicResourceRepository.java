package com.billim.repository;

import com.billim.domain.resource.PublicResource;
import com.billim.domain.resource.ResourceSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PublicResourceRepository
        extends JpaRepository<PublicResource, Long>, PublicResourceRepositoryCustom {

    // 같은 자원을 재수집할 때 중복 저장 대신 갱신(Upsert)하기 위해 필요
    Optional<PublicResource> findBySourceAndExternalId(ResourceSource source, String externalId);

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