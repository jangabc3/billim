package com.billim.domain.reservation;

import com.billim.domain.item.RentalItem;
import com.billim.domain.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 대기 순번은 별도 카운터 컬럼 없이 requestedAt 오름차순 정렬로 계산한다.
 * (ORDER BY requested_at ASC) — 순번을 미리 저장해두면 취소·만료 시 재정렬이 번거로워짐.
 */
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

    private LocalDateTime notifiedAt;   // 반납 발생 시 "예약 확정하세요" 알림 보낸 시각
    private LocalDateTime notifyExpiresAt; // 알림 후 30분 확정 기한

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

    public Long getId() { return id; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public WaitlistStatus getStatus() { return status; }
}
