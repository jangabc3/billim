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

/**
 * 예약 생성/상태 전이를 담당한다.
 * 재고(Inventory)는 낙관적 락(@Version)이 걸려 있어, 동시에 여러 사용자가
 * 마지막 재고 하나를 두고 예약을 시도하면 한쪽만 성공하고 나머지는
 * OptimisticLockingFailureException이 발생한다. 여기서는 소량 재시도로 흡수하고,
 * 재시도를 다 써도 실패하면 "재고 없음"으로 취급해 대기 신청을 유도한다.
 */
@Service
public class ReservationService {

    private static final int MAX_RETRY = 3;
    // "미방문 시 자동 만료" 기준 시각 — 예약 확정 후 24시간 이내 방문하지 않으면 만료 처리한다는 정책 가정.
    private static final long NO_SHOW_EXPIRE_HOURS = 24;

    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;
    private final RentalItemRepository rentalItemRepository;
    private final UserRepository userRepository;

    public ReservationService(InventoryRepository inventoryRepository,
            ReservationRepository reservationRepository,
            RentalItemRepository rentalItemRepository,
            UserRepository userRepository) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.rentalItemRepository = rentalItemRepository;
        this.userRepository = userRepository;
    }

    /** 예약 생성 — 재고 차감과 예약 레코드 생성을 한 트랜잭션으로 묶는다. */
    public Reservation reserve(Long userId, Long rentalItemId) {
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                return reserveOnce(userId, rentalItemId);
            } catch (OptimisticLockingFailureException e) {
                if (attempt == MAX_RETRY) {
                    throw new IllegalStateException("일시적으로 재고 경쟁이 많습니다. 잠시 후 다시 시도해주세요.", e);
                }
                // 재시도 — 다음 루프에서 최신 버전을 다시 읽어와 재계산한다.
            }
        }
        // 도달하지 않는 코드지만 컴파일러를 위해 명시
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

        // 재고 없으면 여기서 IllegalStateException — 낙관적 락 재시도 대상이 아니라 그대로 위로 던진다.
        inventory.reserveOne();

        LocalDateTime expiresAt = LocalDateTime.now().plusHours(NO_SHOW_EXPIRE_HOURS);
        Reservation reservation = new Reservation(user, rentalItem, expiresAt);
        return reservationRepository.save(reservation);
    }

    /** 예약 취소 — 재고를 원복한다. */
    @Transactional
    public void cancel(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다: " + reservationId));
        reservation.cancel();

        inventoryRepository.findByRentalItemId(reservation.getRentalItem().getId())
                .ifPresent(Inventory::releaseOne);
    }

    /** 대여 시작 — 사용자가 실제로 물건을 찾으러 왔을 때, 담당자가 처리한다. */
    @Transactional
    public void startRent(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다: " + reservationId));
        reservation.markRented();

        inventoryRepository.findByRentalItemId(reservation.getRentalItem().getId())
                .orElseThrow(() -> new IllegalStateException("재고 정보가 없습니다."))
                .startRent();
    }

    /** 반납 완료 처리. */
    @Transactional
    public void returnItem(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다: " + reservationId));
        reservation.markReturned();

        inventoryRepository.findByRentalItemId(reservation.getRentalItem().getId())
                .orElseThrow(() -> new IllegalStateException("재고 정보가 없습니다."))
                .returnOne();
    }
}