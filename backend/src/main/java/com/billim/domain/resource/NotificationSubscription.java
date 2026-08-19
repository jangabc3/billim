package com.billim.domain.resource;

import com.billim.domain.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_subscriptions")
public class NotificationSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "public_resource_id", nullable = false)
    private PublicResource publicResource;

    @Column(nullable = false)
    private boolean notifyOnStart;    // 신청 시작 알림

    @Column(nullable = false)
    private boolean notifyOnDeadline; // 마감 임박 알림

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected NotificationSubscription() {
    }

    public NotificationSubscription(User user, PublicResource publicResource,
                                     boolean notifyOnStart, boolean notifyOnDeadline) {
        this.user = user;
        this.publicResource = publicResource;
        this.notifyOnStart = notifyOnStart;
        this.notifyOnDeadline = notifyOnDeadline;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
