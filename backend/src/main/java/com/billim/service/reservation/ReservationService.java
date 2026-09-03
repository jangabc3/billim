package com.billim.service.reservation;

import com.billim.domain.item.Inventory;
import com.billim.domain.item.RentalItem;
import com.billim.domain.reservation.Reservation;
import com.billim.domain.user.User;
import com.billim.repository.InventoryRepository;
import com.billim.repository.RentalItemRepository;
import com.billim.repository.ReservationRepository;
import com.billim.repository.UserRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ReservationService {

    private static final int MAX_RETRY = 3;
    private static final long NO_SHOW_EXPIRE_HOURS = 24;

    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;
    private final RentalItemRepository rentalItemRepository;
    private final UserRepository userRepository;
    private final WaitlistService waitlistService;

    public ReservationService(InventoryRepository inventoryRepository,
            ReservationRepository reservationRepository,
            RentalItemRepository rentalItemRepository,
            UserRepository userRepository,
            WaitlistService waitlistService) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.rentalItemRepository = rentalItemRepository;
        this.userRepository = userRepository;
        this.waitlistService = waitlistService;
    }

    public Reservation reserve(Long userId, Long rentalItemId) {
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                return reserveOnce(userId, rentalItemId);
            } catch (OptimisticLockingFailureException e) {
                if (attempt == MAX_RETRY) {
                    throw new IllegalStateException("일시적으로 재고 경쟁이 많습니다. 잠시 후 다시 시도해주세요.", e);
                }
            }
        }
        throw new IllegalStateException("예약 처리에 실패했습니다.");
    }

    @Transactional
    protected Reservation reserveOnce(Long userId, Long rentalItemId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + userId));
        RentalItem rentalItem = rentalItemRepository.findById(rentalItemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 물품입니다: " + rentalItemId));
        Inventory inventory = inventoryRepository.findByRentalItemId(rentalItemId)
                .orElseThrow(() -> new IllegalStateException("재고 정보가 없습니다: " + rentalItemId));

        inventory.reserveOne();

        LocalDateTime expiresAt = LocalDateTime.now().plusHours(NO_SHOW_EXPIRE_HOURS);
        Reservation reservation = new Reservation(user, rentalItem, expiresAt);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public void cancel(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다: " + reservationId));
        reservation.cancel();

        inventoryRepository.findByRentalItemId(reservation.getRentalItem().getId())
                .ifPresent(Inventory::releaseOne);
    }

    @Transactional
    public void startRent(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다: " + reservationId));
        reservation.markRented();

        inventoryRepository.findByRentalItemId(reservation.getRentalItem().getId())
                .orElseThrow(() -> new IllegalStateException("재고 정보가 없습니다."))
                .startRent();
    }

    @Transactional
    public void returnItem(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다: " + reservationId));
        reservation.markReturned();

        inventoryRepository.findByRentalItemId(reservation.getRentalItem().getId())
                .orElseThrow(() -> new IllegalStateException("재고 정보가 없습니다."))
                .returnOne();

        waitlistService.notifyNextInLine(reservation.getRentalItem().getId());
    }
}