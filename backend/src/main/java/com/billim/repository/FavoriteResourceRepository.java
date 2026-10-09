package com.billim.repository;

import com.billim.domain.resource.FavoriteResource;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteResourceRepository extends JpaRepository<FavoriteResource, Long> {

    /**
     * 즐겨찾기 목록 — 응답에 자원 정보가 필요하므로 publicResource를 한 번의 쿼리로 함께 가져온다.
     * (지연 로딩에 맡기면 컨트롤러에서 LazyInitializationException, 트랜잭션 안이어도 건수만큼 N+1 쿼리 발생)
     */
    @EntityGraph(attributePaths = "publicResource")
    List<FavoriteResource> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<FavoriteResource> findByUserIdAndPublicResourceId(Long userId, Long publicResourceId);

    boolean existsByUserIdAndPublicResourceId(Long userId, Long publicResourceId);
}