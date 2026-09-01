package com.billim.api;

import com.billim.domain.reservation.Reservation;
import com.billim.repository.ReservationRepository;
import com.billim.service.reservation.ReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 인앱 예약(제휴 자원) API.
 * 지금은 인증(JWT)이 아직 없어서 userId를 요청 바디로 직접 받는 임시 형태다.
 * 로그인 기능이 붙으면 인증된 사용자 정보에서 userId를 꺼내오도록 바뀔 예정.
 */
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
    public ResponseEntity<ReservationResponse> reserve(@RequestBody ReservationCreateRequest request) {
        Reservation reservation = reservationService.reserve(request.userId(), request.rentalItemId());
        return ResponseEntity.ok(toResponse(reservation));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        reservationService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    /** 대여 시작 처리 — 담당자가 물건을 내줄 때 호출한다. */
    @PostMapping("/{id}/start")
    public ResponseEntity<Void> startRent(@PathVariable Long id) {
        reservationService.startRent(id);
        return ResponseEntity.noContent().build();
    }

    /** 반납 처리 — 담당자가 물건을 돌려받을 때 호출한다. */
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