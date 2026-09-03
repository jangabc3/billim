package com.billim.api;

import com.billim.domain.reservation.WaitlistStatus;
import java.time.LocalDateTime;

public record WaitlistResponse(
        Long id,
        Long rentalItemId,
        String rentalItemName,
        WaitlistStatus status,
        LocalDateTime requestedAt,
        LocalDateTime notifiedAt,
        LocalDateTime notifyExpiresAt) {
}