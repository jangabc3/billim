package com.billim.api.reservation;

import com.billim.config.security.CustomUserDetails;
import com.billim.domain.reservation.Reservation;
import com.billim.repository.ReservationRepository;
import com.billim.service.reservation.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private final ReservationRepository reservationRepository;

    public ReservationController(ReservationService reservationService,
            ReservationRepository reservationRepository) {
        this.reservationService = reservationService;
        this.reservationRepository = reservationRepository;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> reserve(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ReservationCreateRequest request) {
        Reservation reservation = reservationService.reserve(userDetails.getUserId(), request.rentalItemId());
        return ResponseEntity.ok(toResponse(reservation));
    }

    @GetMapping
    public ResponseEntity<List<ReservationResponse>> getMyReservations(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<ReservationResponse> responses = reservationRepository
                .findByUserIdOrderByCreatedAtDesc(userDetails.getUserId())
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        requireOwnership(userDetails, id);
        reservationService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 대여 시작 처리 — 사용자 본인이 아니라, 물품을 소유한 기관의 담당자(INSTITUTION_ADMIN)
     * 또는 SYSTEM_ADMIN이 현장에서 처리하는 동작이다. 본인 확인이 아니라 기관 관리 권한을 확인한다.
     */
    @PostMapping("/{id}/start")
    public ResponseEntity<Void> startRent(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        requireInstitutionAccess(userDetails, id);
        reservationService.startRent(id);
        return ResponseEntity.noContent().build();
    }

    /** 반납 처리도 startRent와 동일하게 기관 담당자 권한으로 처리한다. */
    @PostMapping("/{id}/return")
    public ResponseEntity<Void> returnItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        requireInstitutionAccess(userDetails, id);
        reservationService.returnItem(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 예약 ID가 요청자 본인 것인지 확인한다. 아니거나 존재하지 않으면 404로 처리해
     * "존재는 하는데 남의 것"이라는 정보조차 노출하지 않는다.
     */
    private void requireOwnership(CustomUserDetails userDetails, Long reservationId) {
        boolean owns = reservationRepository.findById(reservationId)
                .map(r -> r.getUser().getId().equals(userDetails.getUserId()))
                .orElse(false);
        if (!owns) {
            throw new ReservationNotFoundException(reservationId);
        }
    }

    /**
     * 예약이 속한 물품의 기관을 요청자가 관리할 권한이 있는지 확인한다.
     * SYSTEM_ADMIN은 전체 기관, INSTITUTION_ADMIN은 본인이 배정된 기관만 가능하다.
     * 권한이 없으면 403 — 본인 예약이 아니라는 사실 자체를 숨길 필요가 없는 관리자 동작이라
     * requireOwnership처럼 404로 위장하지 않는다.
     */
    private void requireInstitutionAccess(CustomUserDetails userDetails, Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));
        Long institutionId = reservation.getRentalItem().getInstitution().getId();
        if (!userDetails.getUser().canManage(institutionId)) {
            throw new AccessDeniedException("이 기관의 예약을 처리할 권한이 없습니다.");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getOne(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        return reservationRepository.findById(id)
                .filter(r -> r.getUser().getId().equals(userDetails.getUserId()))
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private ReservationResponse toResponse(Reservation r) {
        return new ReservationResponse(
                r.getId(),
                r.getRentalItem().getId(),
                r.getRentalItem().getName(),
                r.getStatus(),
                r.getConfirmedAt(),
                r.getExpiresAt());
    }
}