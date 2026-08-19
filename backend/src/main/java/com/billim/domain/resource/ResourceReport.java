package com.billim.domain.resource;

import com.billim.domain.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "resource_reports")
public class ResourceReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "public_resource_id", nullable = false)
    private PublicResource publicResource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // 익명 제보 허용 시 null 가능

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportType reportType;

    @Column(length = 500)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;

    protected ResourceReport() {
    }

    public ResourceReport(PublicResource publicResource, User user, ReportType reportType, String content) {
        this.publicResource = publicResource;
        this.user = user;
        this.reportType = reportType;
        this.content = content;
        this.status = ReportStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void review(ReportStatus result) {
        this.status = result;
        this.reviewedAt = LocalDateTime.now();
    }
}
