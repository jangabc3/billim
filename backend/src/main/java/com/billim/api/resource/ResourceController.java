package com.billim.api.resource;

import com.billim.domain.item.Category;
import com.billim.domain.resource.ReceptionStatus;
import com.billim.repository.PublicResourceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/resources")
public class ResourceController {

    private static final int MAX_CLOSING_DAYS = 30;

    private final PublicResourceRepository publicResourceRepository;

    public ResourceController(PublicResourceRepository publicResourceRepository) {
        this.publicResourceRepository = publicResourceRepository;
    }

    /**
     * 조건별 검색 — category/gu/receptionStatus/keyword를 조합해서 찾는다.
     * 전부 선택값(null 허용)이라, 아무것도 안 넘기면 전체 목록을 페이징해서 보여주는 것과 같다.
     *
     * closingWithinDays: 지금부터 N일(1~30) 안에 접수가 끝나는 자원만, 마감이 가까운 순으로 돌려준다.
     */
    @GetMapping("/search")
    public Page<PublicResourceResponse> search(
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) String gu,
            @RequestParam(required = false) ReceptionStatus receptionStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean freeOnly,
            @RequestParam(required = false) Integer closingWithinDays,
            Pageable pageable) {
        Integer days = closingWithinDays == null
                ? null
                : Math.min(Math.max(closingWithinDays, 1), MAX_CLOSING_DAYS);

        return publicResourceRepository
                .search(category, gu, receptionStatus, keyword, freeOnly, days, pageable)
                .map(PublicResourceResponse::from);
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
                .map(PublicResourceResponse::from);
    }

    /**
     * 자원 하나만 정확히 조회. 상세 화면이 이걸 쓴다.
     * 없으면 404를 정직하게 돌려준다 — 빈 데이터로 얼버무리지 않는다.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PublicResourceResponse> getOne(@PathVariable Long id) {
        return publicResourceRepository.findById(id)
                .map(PublicResourceResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}