package com.billim.domain.item;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 재고 관리. @Version으로 낙관적 락을 걸어서,
 * 마지막 재고 1개에 두 사용자가 동시에 예약 버튼을 눌러도
 * 한 명만 성공하고 다른 한 명은 재시도(또는 대기 신청 안내)하도록 한다.
 */
@Entity
@Table(name = "inventories")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_item_id", nullable = false, unique = true)
    private RentalItem rentalItem;

    @Column(nullable = false)
    private int totalQuantity;

    @Column(nullable = false)
    private int availableQuantity;

    @Column(nullable = false)
    private int reservedQuantity;

    @Column(nullable = false)
    private int rentedQuantity;

    @Version
    private Long version; // 낙관적 락 — 동시 수정 시 OptimisticLockException 발생

    private LocalDateTime updatedAt;

    protected Inventory() {
    }

    public Inventory(RentalItem rentalItem, int totalQuantity) {
        this.rentalItem = rentalItem;
        this.totalQuantity = totalQuantity;
        this.availableQuantity = totalQuantity;
        this.reservedQuantity = 0;
        this.rentedQuantity = 0;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /** 예약 확정: 대여 가능 수량을 예약 수량으로 이동. 재고 없으면 예외. */
    public void reserveOne() {
        if (availableQuantity <= 0) {
            throw new IllegalStateException("재고가 없습니다. 대기 신청이 필요합니다.");
        }
        availableQuantity -= 1;
        reservedQuantity += 1;
    }

    /** 예약 취소·만료: 예약 수량을 다시 대여 가능 수량으로 되돌림. */
    public void releaseOne() {
        reservedQuantity -= 1;
        availableQuantity += 1;
    }

    /** 대여 시작: 예약 수량에서 대여중 수량으로 이동. */
    public void startRent() {
        reservedQuantity -= 1;
        rentedQuantity += 1;
    }

    /** 반납: 대여중 수량을 다시 대여 가능 수량으로. */
    public void returnOne() {
        rentedQuantity -= 1;
        availableQuantity += 1;
    }

    public int getAvailableQuantity() { return availableQuantity; }
    public Long getVersion() { return version; }
}
