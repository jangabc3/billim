package com.billim.service.reservation;

import com.billim.domain.item.Inventory;
import com.billim.domain.item.RentalItem;
import com.billim.domain.reservation.Reservation;
import com.billim.domain.user.User;
import com.billim.repository.InventoryRepository;
import com.billim.repository.RentalItemRepository;
import com.billim.repository.ReservationRepository;
import com.billim.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * ReservationService.reserve()의 재시도 루프 안에서 호출되는 "예약 1회 시도"를 담당한다.
 * 별도 빈으로 분리한 이유: 같은 클래스 안에서 this.reserveOnce()로 호출하면 스프링 AOP 프록시를
 * 거치지 않아 @Transactional이 무시된다(self-invocation 문제). 이 클래스를 통해 진짜 프록시를
 * 거치게 만들어야 트랜잭션 경계와 낙관적 락 버전 체크가 실제로 걸린다.
 */
@Component
public class InventoryReservationExecutor {

    private static final long NO_SHOW_EXPIRE_HOURS = 24;

    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;
    private final RentalItemRepository rentalItemRepository;
    private final UserRepository userRepository;

    public InventoryReservationExecutor(InventoryRepository inventoryRepository,
            ReservationRepository reservationRepository,
            RentalItemRepository rentalItemRepository,
            UserRepository userRepository) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.rentalItemRepository = rentalItemRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Reservation reserveOnce(Long userId, Long rentalItemId) {
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
}