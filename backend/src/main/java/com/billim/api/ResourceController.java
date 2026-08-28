package com.billim.api;

import com.billim.domain.item.Category;
import com.billim.domain.resource.PublicResource;
import com.billim.domain.resource.ReceptionStatus;
import com.billim.repository.PublicResourceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/api/v1/resources")
public class ResourceController {

    private final PublicResourceRepository publicResourceRepository;

    public ResourceController(PublicResourceRepository publicResourceRepository) {
        this.publicResourceRepository = publicResourceRepository;
    }

    /**
     * 전체 목록 — 임시로 남겨둔 가장 단순한 버전. 실제 화면은 이제 /search를 쓴다.
     */
    @GetMapping
    public List<PublicResourceResponse> list() {
        return publicResourceRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 조건별 검색 — category/gu/receptionStatus/keyword를 조합해서 찾는다.
     * 전부 선택값(null 허용)이라, 아무것도 안 넘기면 전체 목록을 페이징해서 보여주는 것과 같다.
     */
    @GetMapping("/search")
    public Page<PublicResourceResponse> search(
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) String gu,
            @RequestParam(required = false) ReceptionStatus receptionStatus,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        return publicResourceRepository
                .search(category, gu, receptionStatus, keyword, pageable)
                .map(this::toResponse);
    }

    /**
     * 반경 검색 — 중심좌표(lat, lng)에서 radiusMeters(미터) 이내의 자원을 찾는다.
     * PostGIS ST_DWithin 기반. "내 주변" 검색 화면이 이걸 쓴다.
     */
    @GetMapping("/nearby")
    public Page<PublicResourceResponse> nearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "1000") double radiusMeters,
            Pageable pageable) {
        return publicResourceRepository
                .findNearby(lat, lng, radiusMeters, pageable)
                .map(this::toResponse);
    }

    /**
     * 자원 하나만 정확히 조회. 상세 화면이 이걸 쓴다.
     * 없으면 404를 정직하게 돌려준다 — 빈 데이터로 얼버무리지 않는다.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PublicResourceResponse> getOne(@PathVariable Long id) {
        return publicResourceRepository.findById(id)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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