package com.billim.service.reservation;

import com.billim.domain.item.RentalItem;
import com.billim.domain.reservation.Reservation;
import com.billim.domain.reservation.WaitlistEntry;
import com.billim.domain.reservation.WaitlistStatus;
import com.billim.domain.user.User;
import com.billim.repository.InventoryRepository;
import com.billim.repository.RentalItemRepository;
import com.billim.repository.ReservationRepository;
import com.billim.repository.UserRepository;
import com.billim.repository.WaitlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class WaitlistService {

    private static final long NO_SHOW_EXPIRE_HOURS = 24;

    private final WaitlistRepository waitlistRepository;
    private final RentalItemRepository rentalItemRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final InventoryRepository inventoryRepository;

    public WaitlistService(WaitlistRepository waitlistRepository,
            RentalItemRepository rentalItemRepository,
            UserRepository userRepository,
            ReservationRepository reservationRepository,
            InventoryRepository inventoryRepository) {
        this.waitlistRepository = waitlistRepository;
        this.rentalItemRepository = rentalItemRepository;
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public WaitlistEntry join(Long userId, Long rentalItemId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + userId));
        RentalItem rentalItem = rentalItemRepository.findById(rentalItemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 물품입니다: " + rentalItemId));

        WaitlistEntry entry = new WaitlistEntry(user, rentalItem);
        return waitlistRepository.save(entry);
    }

    @Transactional
    public void cancel(Long waitlistId) {
        WaitlistEntry entry = waitlistRepository.findById(waitlistId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 대기 신청입니다: " + waitlistId));
        entry.cancel();
    }

    @Transactional
    public void notifyNextInLine(Long rentalItemId) {
        Optional<WaitlistEntry> next = waitlistRepository
                .findFirstByRentalItemIdAndStatusOrderByRequestedAtAsc(rentalItemId, WaitlistStatus.WAITING);
        next.ifPresent(WaitlistEntry::notifyAvailable);
    }

    @Transactional
    public Reservation confirm(Long waitlistId) {
        WaitlistEntry entry = waitlistRepository.findById(waitlistId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 대기 신청입니다: " + waitlistId));

        if (entry.getStatus() != WaitlistStatus.NOTIFIED) {
            throw new IllegalStateException("알림을 받은 대기 신청만 확정할 수 있습니다.");
        }

        entry.convertToReservation();

        inventoryRepository.findByRentalItemId(entry.getRentalItem().getId())
                .orElseThrow(() -> new IllegalStateException("재고 정보가 없습니다."))
                .reserveOne();

        LocalDateTime expiresAt = LocalDateTime.now().plusHours(NO_SHOW_EXPIRE_HOURS);
        Reservation reservation = new Reservation(entry.getUser(), entry.getRentalItem(), expiresAt);
        return reservationRepository.save(reservation);
    }
}