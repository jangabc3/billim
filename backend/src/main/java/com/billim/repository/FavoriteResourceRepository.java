package com.billim.repository;

import com.billim.domain.resource.FavoriteResource;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FavoriteResourceRepository extends JpaRepository<FavoriteResource, Long> {

    @EntityGraph(attributePaths = "publicResource")
    List<FavoriteResource> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<FavoriteResource> findByUserIdAndPublicResourceId(Long userId, Long publicResourceId);

    boolean existsByUserIdAndPublicResourceId(Long userId, Long publicResourceId);

    /**
     * 즐겨찾기를 DB에서 원자적으로 추가한다. 이미 있으면 아무 일도 하지 않는다(0 반환).
     * "있는지 확인 → 저장" 두 단계로 나누면 동시 요청 두 개가 둘 다 확인을 통과해 유니크 제약 위반(500)이 나므로,
     * 충돌 처리를 DB에 맡긴다.
     */
    @Modifying
    @Query(value = """
            insert into favorite_resources (user_id, public_resource_id, created_at)
            values (:userId, :resourceId, :now)
            on conflict do nothing
            """, nativeQuery = true)
    int insertIgnore(@Param("userId") Long userId,
            @Param("resourceId") Long resourceId,
            @Param("now") LocalDateTime now);
}