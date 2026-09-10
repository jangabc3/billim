package com.billim.api;

import com.billim.config.security.CustomUserDetails;
import com.billim.domain.resource.FavoriteResource;
import com.billim.domain.resource.PublicResource;
import com.billim.service.resource.FavoriteResourceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 즐겨찾기 API. 인증 필요 — 프론트의 로컬 상태(BookmarkContext)를 서버와 동기화하기 위한 것.
 */
@RestController
@RequestMapping("/api/v1/favorites")
public class FavoriteResourceController {

    private final FavoriteResourceService favoriteResourceService;

    public FavoriteResourceController(FavoriteResourceService favoriteResourceService) {
        this.favoriteResourceService = favoriteResourceService;
    }

    @GetMapping
    public List<PublicResourceResponse> list(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return favoriteResourceService.list(userDetails.getUserId()).stream()
                .map(FavoriteResource::getPublicResource)
                .map(this::toResponse)
                .toList();
    }

    @PostMapping("/{resourceId}")
    public ResponseEntity<Void> add(@AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long resourceId) {
        favoriteResourceService.add(userDetails.getUserId(), resourceId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{resourceId}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long resourceId) {
        favoriteResourceService.remove(userDetails.getUserId(), resourceId);
        return ResponseEntity.noContent().build();
    }

    private PublicResourceResponse toResponse(PublicResource r) {
        return new PublicResourceResponse(
                r.getId(),
                r.getSource(),
                r.getName(),
                r.getCategory(),
                r.getAddress(),
                r.getGu(),
                r.getLatitude(),
                r.getLongitude(),
                r.getFee(),
                r.getReceptionStatus(),
                r.getReceptionEndAt(),
                r.getReservationType(),
                r.getReservationUrl(),
                r.getImageUrl(),
                r.getLastSyncedAt());
    }
}