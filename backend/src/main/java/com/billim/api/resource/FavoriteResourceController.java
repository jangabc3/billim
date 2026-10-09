package com.billim.api.resource;

import com.billim.config.security.CustomUserDetails;
import com.billim.domain.resource.FavoriteResource;
import com.billim.service.resource.FavoriteResourceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 즐겨찾기 API. 인증 필요 — 프론트의 로컬 상태(BookmarkContext)를 서버와 동기화하기 위한 것.
 * 마감된 자원도 목록에서 빼지 않는다 — receptionStatus가 CLOSED로 내려가므로 화면에서 "마감"으로 표시한다.
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
                .map(PublicResourceResponse::from)
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
}