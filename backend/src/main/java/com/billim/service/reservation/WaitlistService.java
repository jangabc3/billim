package com.billim.service.reservation;

import com.billim.common.error.ApiException;
import com.billim.common.error.ErrorCode;
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
import java.util.List;
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
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        RentalItem rentalItem = rentalItemRepository.findById(rentalItemId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "존재하지 않는 물품입니다."));

        WaitlistEntry entry = new WaitlistEntry(user, rentalItem);
        return waitlistRepository.save(entry);
    }

    @Transactional
    public void cancel(Long waitlistId) {
        findEntry(waitlistId).cancel();
    }

    @Transactional
    public void notifyNextInLine(Long rentalItemId) {
        Optional<WaitlistEntry> next = waitlistRepository
                .findFirstByRentalItemIdAndStatusOrderByRequestedAtAsc(rentalItemId, WaitlistStatus.WAITING);
        next.ifPresent(WaitlistEntry::notifyAvailable);
    }

    @Transactional
    public Reservation confirm(Long waitlistId) {
        WaitlistEntry entry = findEntry(waitlistId);

        if (entry.getStatus() != WaitlistStatus.NOTIFIED) {
            throw new ApiException(ErrorCode.WAITLIST_NOT_NOTIFIED);
        }

        entry.convertToReservation();

        Long rentalItemId = entry.getRentalItem().getId();
        inventoryRepository.findByRentalItemId(rentalItemId)
                .orElseThrow(() -> new ApiException(ErrorCode.COMMON_INTERNAL_ERROR,
                        "재고 정보가 없습니다. rentalItemId=" + rentalItemId))
                .reserveOne();

        LocalDateTime expiresAt = LocalDateTime.now().plusHours(NO_SHOW_EXPIRE_HOURS);
        Reservation reservation = new Reservation(entry.getUser(), entry.getRentalItem(), expiresAt);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public void expireOverdueNotifications() {
        List<WaitlistEntry> overdue = waitlistRepository
                .findByStatusAndNotifyExpiresAtBefore(WaitlistStatus.NOTIFIED, LocalDateTime.now());

        for (WaitlistEntry entry : overdue) {
            entry.expireNotification();
            notifyNextInLine(entry.getRentalItem().getId());
        }
    }

    private WaitlistEntry findEntry(Long waitlistId) {
        return waitlistRepository.findById(waitlistId)
                .orElseThrow(() -> new ApiException(ErrorCode.WAITLIST_NOT_FOUND));
    }
}