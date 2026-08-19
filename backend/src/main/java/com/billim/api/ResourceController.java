package com.billim.api;

import com.billim.domain.resource.PublicResource;
import com.billim.repository.PublicResourceRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/resources")
public class ResourceController {

    private final PublicResourceRepository publicResourceRepository;

    public ResourceController(PublicResourceRepository publicResourceRepository) {
        this.publicResourceRepository = publicResourceRepository;
    }

    /**
     * 지금은 DB에 있는 걸 전부 반환하는 가장 단순한 버전.
     * 다음 단계에서 category/gu/freeOnly/radius 쿼리 파라미터를 받는 검색으로 확장 예정.
     */
    @GetMapping
    public List<PublicResourceResponse> list() {
        return publicResourceRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
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
                r.getLastSyncedAt()
        );
    }
}
