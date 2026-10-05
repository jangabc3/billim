package com.billim.api.reservation;

import com.billim.config.security.CustomUserDetails;
import com.billim.domain.reservation.WaitlistEntry;
import com.billim.repository.WaitlistRepository;
import com.billim.service.reservation.WaitlistService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/waitlist")
public class WaitlistController {

    private final WaitlistService waitlistService;
    private final WaitlistRepository waitlistRepository;

    public WaitlistController(WaitlistService waitlistService, WaitlistRepository waitlistRepository) {
        this.waitlistService = waitlistService;
        this.waitlistRepository = waitlistRepository;
    }

    @PostMapping
    public ResponseEntity<WaitlistResponse> join(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody WaitlistJoinRequest request) {
        WaitlistEntry entry = waitlistService.join(userDetails.getUserId(), request.rentalItemId());
        return ResponseEntity.ok(toResponse(entry));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        requireOwnership(userDetails, id);
        waitlistService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<Void> confirm(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        requireOwnership(userDetails, id);
        waitlistService.confirm(id);
        return ResponseEntity.ok().build();
    }

    private void requireOwnership(CustomUserDetails userDetails, Long waitlistId) {
        boolean owns = waitlistRepository.findById(waitlistId)
                .map(w -> w.getUser().getId().equals(userDetails.getUserId()))
                .orElse(false);
        if (!owns) {
            throw new WaitlistNotFoundException(waitlistId);
        }
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