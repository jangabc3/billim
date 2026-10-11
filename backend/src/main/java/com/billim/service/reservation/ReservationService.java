package com.billim.service.reservation;

import com.billim.common.error.ApiException;
import com.billim.common.error.ErrorCode;
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
 * 재시도를 다 써도 실패하면 RESERVATION_CONTENTION(409)으로 응답한다.
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
                    throw new ApiException(ErrorCode.RESERVATION_CONTENTION, e);
                }
                // 재시도 — 다음 루프에서 최신 버전을 다시 읽어와 재계산한다.
            }
        }
        // 위 루프는 항상 return 또는 throw로 끝나므로 도달하지 않는다.
        throw new ApiException(ErrorCode.COMMON_INTERNAL_ERROR);
    }

    /** 예약 취소 — 재고를 원복한다. */
    @Transactional
    public void cancel(Long reservationId) {
        Reservation reservation = findReservation(reservationId);
        reservation.cancel();

        inventoryRepository.findByRentalItemId(reservation.getRentalItem().getId())
                .ifPresent(Inventory::releaseOne);
    }

    /** 대여 시작 — 사용자가 실제로 물건을 찾으러 왔을 때, 담당자가 처리한다. */
    @Transactional
    public void startRent(Long reservationId) {
        Reservation reservation = findReservation(reservationId);
        reservation.markRented();

        findInventory(reservation.getRentalItem().getId()).startRent();
    }

    /** 반납 완료 처리 — 재고를 원복하고, 대기자가 있으면 1순위에게 알림을 보낸다. */
    @Transactional
    public void returnItem(Long reservationId) {
        Reservation reservation = findReservation(reservationId);
        reservation.markReturned();

        findInventory(reservation.getRentalItem().getId()).returnOne();

        waitlistService.notifyNextInLine(reservation.getRentalItem().getId());
    }

    private Reservation findReservation(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESERVATION_NOT_FOUND));
    }

    /** 재고 레코드가 없는 건 사용자 잘못이 아니라 데이터 불일치라서 500으로 처리한다(상세는 로그에만). */
    private Inventory findInventory(Long rentalItemId) {
        return inventoryRepository.findByRentalItemId(rentalItemId)
                .orElseThrow(() -> new ApiException(ErrorCode.COMMON_INTERNAL_ERROR,
                        "재고 정보가 없습니다. rentalItemId=" + rentalItemId));
    }
}