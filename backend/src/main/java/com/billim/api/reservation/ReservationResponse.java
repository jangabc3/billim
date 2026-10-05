package com.billim.api.reservation;

import com.billim.domain.reservation.ReservationStatus;
import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        Long rentalItemId,
        String rentalItemName,
        ReservationStatus status,
        LocalDateTime confirmedAt,
        LocalDateTime expiresAt) {
}