package com.billim.api.resource;

import com.billim.common.Times;
import com.billim.domain.item.Category;
import com.billim.domain.resource.PublicResource;
import com.billim.domain.resource.ReceptionStatus;
import com.billim.domain.resource.ReservationType;
import com.billim.domain.resource.ResourceSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 엔티티를 API 응답에 그대로 내보내지 않고 DTO로 감싼다.
 * 나중에 엔티티 필드가 바뀌어도 API 계약(응답 형태)이 흔들리지 않게 하기 위함.
 */
public record PublicResourceResponse(
        Long id,
        ResourceSource source,
        String name,
        Category category,
        String address,
        String gu,
        BigDecimal latitude,
        BigDecimal longitude,
        String fee,
        String subCategory,
        ReceptionStatus receptionStatus,
        LocalDateTime receptionEndAt,
        ReservationType reservationType,
        String reservationUrl,
        String imageUrl,
        LocalDateTime lastSyncedAt) {

    /** receptionStatus는 저장값이 아니라 "지금 시각 기준으로 보정한 상태"를 담는다. */
    public static PublicResourceResponse from(PublicResource r) {
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
                r.getSubCategory(),
                r.effectiveReceptionStatus(Times.now()),
                r.getReceptionEndAt(),
                r.getReservationType(),
                r.getReservationUrl(),
                r.getImageUrl(),
                r.getLastSyncedAt());
    }
}