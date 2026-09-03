package com.billim.api;

import com.billim.config.security.CustomUserDetails;
import com.billim.domain.reservation.Reservation;
import com.billim.repository.ReservationRepository;
import com.billim.service.reservation.ReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
            @RequestBody ReservationCreateRequest request) {
        Reservation reservation = reservationService.reserve(userDetails.getUserId(), request.rentalItemId());
        return ResponseEntity.ok(toResponse(reservation));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        reservationService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<Void> startRent(@PathVariable Long id) {
        reservationService.startRent(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/return")
    public ResponseEntity<Void> returnItem(@PathVariable Long id) {
        reservationService.returnItem(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getOne(@PathVariable Long id) {
        return reservationRepository.findById(id)
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