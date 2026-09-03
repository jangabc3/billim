package com.billim.domain.reservation;

import com.billim.domain.item.RentalItem;
import com.billim.domain.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "waitlist_entries")
public class WaitlistEntry {

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
    private WaitlistStatus status;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime notifiedAt;
    private LocalDateTime notifyExpiresAt;

    protected WaitlistEntry() {
    }

    public WaitlistEntry(User user, RentalItem rentalItem) {
        this.user = user;
        this.rentalItem = rentalItem;
        this.status = WaitlistStatus.WAITING;
        this.requestedAt = LocalDateTime.now();
    }

    public void notifyAvailable() {
        this.status = WaitlistStatus.NOTIFIED;
        this.notifiedAt = LocalDateTime.now();
        this.notifyExpiresAt = this.notifiedAt.plusMinutes(30);
    }

    public void convertToReservation() {
        this.status = WaitlistStatus.CONVERTED;
    }

    public void expireNotification() {
        this.status = WaitlistStatus.EXPIRED;
    }

    public void cancel() {
        if (this.status == WaitlistStatus.CONVERTED || this.status == WaitlistStatus.EXPIRED) {
            throw new IllegalStateException("이미 종료된 대기 신청은 취소할 수 없습니다.");
        }
        this.status = WaitlistStatus.EXPIRED;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public RentalItem getRentalItem() {
        return rentalItem;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public LocalDateTime getNotifiedAt() {
        return notifiedAt;
    }

    public LocalDateTime getNotifyExpiresAt() {
        return notifyExpiresAt;
    }

    public WaitlistStatus getStatus() {
        return status;
    }
}