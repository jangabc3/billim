package com.billim.repository;

import com.billim.domain.reservation.WaitlistEntry;
import com.billim.domain.reservation.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {

    List<WaitlistEntry> findByRentalItemIdAndStatusOrderByRequestedAtAsc(
            Long rentalItemId, WaitlistStatus status);

    Optional<WaitlistEntry> findFirstByRentalItemIdAndStatusOrderByRequestedAtAsc(
            Long rentalItemId, WaitlistStatus status);
}