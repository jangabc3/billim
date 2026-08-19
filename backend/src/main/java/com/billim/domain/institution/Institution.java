package com.billim.domain.institution;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 빌림이 직접 관리하는 제휴 기관 (예: 성수2가 주민센터, 성동청년지원센터).
 * 공유누리·서울시처럼 외부 API로 조회만 되는 기관과 달리,
 * 이 기관의 물품(RentalItem)은 앱에서 직접 예약이 가능하다.
 */
@Entity
@Table(name = "institutions")
public class Institution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // CSV 재업로드 시 Upsert 기준이 되는 값 (예: CENTER-001)
    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(nullable = false, length = 200)
    private String address;

    @Column(nullable = false, length = 20)
    private String gu;   // 예: 성동구, 마포구 — 검색 필터의 핵심 컬럼

    @Column(length = 20)
    private String dong; // 예: 성수2가동

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String operatingHours;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    protected Institution() {
    }

    public Institution(String code, String name, String address, String gu, String dong,
                        BigDecimal latitude, BigDecimal longitude) {
        this.code = code;
        this.name = name;
        this.address = address;
        this.gu = gu;
        this.dong = dong;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // CSV 재업로드 시 이 메서드로 기존 row를 갱신 (Upsert)
    public void updateFrom(String name, String address, String gu, String dong,
                            BigDecimal latitude, BigDecimal longitude) {
        this.name = name;
        this.address = address;
        this.gu = gu;
        this.dong = dong;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getGu() { return gu; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
}
