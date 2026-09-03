package com.billim.api;

import com.billim.config.security.CustomUserDetails;
import com.billim.domain.reservation.WaitlistEntry;
import com.billim.service.reservation.WaitlistService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/waitlist")
public class WaitlistController {

    private final WaitlistService waitlistService;

    public WaitlistController(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    @PostMapping
    public ResponseEntity<WaitlistResponse> join(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody WaitlistJoinRequest request) {
        WaitlistEntry entry = waitlistService.join(userDetails.getUserId(), request.rentalItemId());
        return ResponseEntity.ok(toResponse(entry));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        waitlistService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<Void> confirm(@PathVariable Long id) {
        waitlistService.confirm(id);
        return ResponseEntity.ok().build();
    }

    private WaitlistResponse toResponse(WaitlistEntry entry) {
        return new WaitlistResponse(
                entry.getId(),
                entry.getRentalItem().getId(),
                entry.getRentalItem().getName(),
                entry.getStatus(),
                entry.getRequestedAt(),
                entry.getNotifiedAt(),
                entry.getNotifyExpiresAt());
    }
}