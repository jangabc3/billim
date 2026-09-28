package com.billim.api;

import jakarta.validation.constraints.NotNull;

public record WaitlistJoinRequest(
        @NotNull(message = "물품 ID는 필수입니다.") Long rentalItemId) {
}