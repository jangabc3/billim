package com.billim.api;

import com.billim.config.security.CustomUserDetails;
import com.billim.domain.reservation.Reservation;
import com.billim.repository.ReservationRepository;
import com.billim.service.reservation.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/{id}/start")
    public ResponseEntity<Void> startRent(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        requireOwnership(userDetails, id);
        reservationService.startRent(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/return")
    public ResponseEntity<Void> returnItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        requireOwnership(userDetails, id);
        reservationService.returnItem(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 예약 ID가 요청자 본인 것인지 확인한다. 아니거나 존재하지 않으면 404로 처리해
     * "존재는 하는데 남의 것"이라는 정보조차 노출하지 않는다 — getOne에서 이미 쓰던
     * 정보 노출 최소화 패턴을 cancel/start/return에도 동일하게 적용한다.
     */
    private void requireOwnership(CustomUserDetails userDetails, Long reservationId) {
        boolean owns = reservationRepository.findById(reservationId)
                .map(r -> r.getUser().getId().equals(userDetails.getUserId()))
                .orElse(false);
        if (!owns) {
            throw new ReservationNotFoundException(reservationId);
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