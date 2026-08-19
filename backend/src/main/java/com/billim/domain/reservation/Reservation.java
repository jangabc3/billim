package com.billim.domain.reservation;

import com.billim.domain.item.RentalItem;
import com.billim.domain.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_item_id", nullable = false)
    private RentalItem rentalItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    private LocalDateTime confirmedAt;
    private LocalDateTime expiresAt;  // 미방문 시 자동 만료 기준 시각 (Scheduler가 체크)
    private LocalDateTime rentedAt;
    private LocalDateTime returnedAt;

    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Reservation() {
    }

    public Reservation(User user, RentalItem rentalItem, LocalDateTime expiresAt) {
        this.user = user;
        this.rentalItem = rentalItem;
        this.status = ReservationStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /** 상태 전이 규칙: 정해진 순서 밖의 전이는 막는다. */
    public void markRented() {
        require(ReservationStatus.CONFIRMED);
        this.status = ReservationStatus.RENTED;
        this.rentedAt = LocalDateTime.now();
    }

    public void markReturned() {
        require(ReservationStatus.RENTED);
        this.status = ReservationStatus.RETURNED;
        this.returnedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (this.status == ReservationStatus.RETURNED || this.status == ReservationStatus.CANCELED) {
            throw new IllegalStateException("이미 종료된 예약은 취소할 수 없습니다.");
        }
        this.status = ReservationStatus.CANCELED;
    }

    public void expire() {
        require(ReservationStatus.CONFIRMED);
        this.status = ReservationStatus.EXPIRED;
    }

    private void require(ReservationStatus expected) {
        if (this.status != expected) {
            throw new IllegalStateException(
                String.format("%s 상태에서만 가능한 전이입니다. 현재 상태: %s", expected, this.status));
        }
    }

    public Long getId() { return id; }
    public ReservationStatus getStatus() { return status; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}
