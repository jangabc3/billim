package com.billim.service.reservation;

import com.billim.domain.item.Inventory;
import com.billim.domain.reservation.Reservation;
import com.billim.repository.InventoryRepository;
import com.billim.repository.ReservationRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 예약 생성/상태 전이를 담당한다.
 * 재고(Inventory)는 낙관적 락(@Version)이 걸려 있어, 동시에 여러 사용자가
 * 마지막 재고 하나를 두고 예약을 시도하면 한쪽만 성공하고 나머지는
 * OptimisticLockingFailureException이 발생한다. 여기서는 소량 재시도로 흡수하고,
 * 재시도를 다 써도 실패하면 "재고 없음"으로 취급해 대기 신청을 유도한다.
 *
 * 실제 예약 1회 시도(reserveOnce)는 InventoryReservationExecutor에 위임한다 —
 * 같은 클래스 안에서 @Transactional 메서드를 this.로 호출하면 스프링 프록시를 우회해서
 * 트랜잭션이 걸리지 않는 self-invocation 문제 때문에, 별도 빈으로 분리했다.
 */
@Service
public class ReservationService {

    private static final int MAX_RETRY = 3;

    private final InventoryReservationExecutor reservationExecutor;
    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;
    private final WaitlistService waitlistService;

    public ReservationService(InventoryReservationExecutor reservationExecutor,
            InventoryRepository inventoryRepository,
            ReservationRepository reservationRepository,
            WaitlistService waitlistService) {
        this.reservationExecutor = reservationExecutor;
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.waitlistService = waitlistService;
    }

    /** 예약 생성 — 재고 차감과 예약 레코드 생성을 한 트랜잭션으로 묶는다. */
    public Reservation reserve(Long userId, Long rentalItemId) {
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                return reservationExecutor.reserveOnce(userId, rentalItemId);
            } catch (OptimisticLockingFailureException e) {
                if (attempt == MAX_RETRY) {
                    throw new IllegalStateException("일시적으로 재고 경쟁이 많습니다. 잠시 후 다시 시도해주세요.", e);
                }
                // 재시도 — 다음 루프에서 최신 버전을 다시 읽어와 재계산한다.
            }
        }
        throw new IllegalStateException("예약 처리에 실패했습니다.");
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

    /** 반납 완료 처리 — 재고를 원복하고, 대기자가 있으면 1순위에게 알림을 보낸다. */
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