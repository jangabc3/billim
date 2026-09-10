package com.billim.repository;

import com.billim.domain.resource.FavoriteResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteResourceRepository extends JpaRepository<FavoriteResource, Long> {

    List<FavoriteResource> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<FavoriteResource> findByUserIdAndPublicResourceId(Long userId, Long publicResourceId);

    boolean existsByUserIdAndPublicResourceId(Long userId, Long publicResourceId);
}