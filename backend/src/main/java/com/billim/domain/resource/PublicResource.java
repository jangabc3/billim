package com.billim.domain.resource;

import com.billim.domain.item.Category;
import com.billim.domain.item.RentalItem;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 검색·목록·상세 화면이 바라보는 단일 모델.
 * 공유누리·서울시에서 수집한 자원과, 빌림이 직접 관리하는 제휴 기관 물품을
 * 이 테이블 하나로 통합해서 화면단은 출처를 신경 쓰지 않고 조회할 수 있게 한다.
 *
 * (source, externalId) 유니크 제약으로 같은 데이터를 여러 번 수집해도 중복 저장되지 않고
 * Upsert(신규면 INSERT, 이미 있으면 UPDATE)로 처리한다.
 */
@Entity
@Table(name = "public_resources", uniqueConstraints = @UniqueConstraint(columnNames = { "source", "external_id" }))
public class PublicResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResourceSource source;

    // BILLIM_PARTNER는 외부 ID가 없으므로 rentalItem.id를 문자열로 사용
    @Column(name = "external_id", nullable = false, length = 100)
    private String externalId;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @Column(nullable = false, length = 200)
    private String address;

    @Column(nullable = false, length = 20)
    private String gu;

    @Column(length = 20)
    private String dong;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    // 반경 검색 전용 — GIST 인덱스로 ST_DWithin 조회에 사용. 화면 표시는 위의 lat/lng를 그대로 씀.
    @Column(columnDefinition = "geography(Point,4326)")
    private Point location;

    @Column(length = 30)
    private String fee; // "무료" / "3,000원" / null(확인 필요)

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ReceptionStatus receptionStatus; // OPEN, CLOSING_SOON, CLOSED, UNKNOWN

    // 서울시 API의 RCPTENDDT처럼 접수 마감 시각을 주는 출처가 있어서 별도 저장.
    // "오늘 마감"/"D-1" 배지는 이 값을 기준으로 화면단(또는 조회 시점)에서 계산한다.
    private LocalDateTime receptionEndAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationType reservationType; // EXTERNAL_LINK, DIRECT_BOOKING

    private String reservationUrl; // EXTERNAL_LINK인 경우 공식 예약 페이지

    // DIRECT_BOOKING인 경우에만 값 존재 — 빌림 자체 예약 플로우로 연결되는 다리
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_item_id")
    private RentalItem rentalItem;

    private String imageUrl;
    private String phone; // 실제 문의 전화 — 서울시 API TELNO
    private String operatingHours; // 예: "07:00 ~ 19:00" — 서울시 API V_MIN/V_MAX 조합

    private LocalDateTime externalUpdatedAt; // 원본 기관에서의 마지막 수정 시각
    private LocalDateTime lastSyncedAt; // 빌림이 마지막으로 수집한 시각

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected PublicResource() {
    }

    public static PublicResource fromExternal(ResourceSource source, String externalId, String name,
            Category category, String address, String gu, String dong,
            BigDecimal latitude, BigDecimal longitude,
            String fee, ReceptionStatus receptionStatus,
            LocalDateTime receptionEndAt,
            String reservationUrl, String imageUrl,
            String phone, String operatingHours,
            LocalDateTime externalUpdatedAt) {
        PublicResource r = new PublicResource();
        r.source = source;
        r.externalId = externalId;
        r.name = name;
        r.category = category;
        r.address = address;
        r.gu = gu;
        r.dong = dong;
        r.latitude = latitude;
        r.longitude = longitude;
        r.fee = fee;
        r.receptionStatus = receptionStatus;
        r.receptionEndAt = receptionEndAt;
        r.reservationType = ReservationType.EXTERNAL_LINK;
        r.reservationUrl = reservationUrl;
        r.imageUrl = imageUrl;
        r.phone = phone;
        r.operatingHours = operatingHours;
        r.externalUpdatedAt = externalUpdatedAt;
        r.lastSyncedAt = LocalDateTime.now();
        return r;
    }

    /** 외부 API 재수집 시 같은 (source, externalId) row를 이 메서드로 갱신한다 (Upsert). */
    public void syncFromExternal(String name, String address, String fee,
            ReceptionStatus receptionStatus, String imageUrl,
            LocalDateTime externalUpdatedAt) {
        this.name = name;
        this.address = address;
        this.fee = fee;
        this.receptionStatus = receptionStatus;
        this.imageUrl = imageUrl;
        this.externalUpdatedAt = externalUpdatedAt;
        this.lastSyncedAt = LocalDateTime.now();
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public ResourceSource getSource() {
        return source;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getName() {
        return name;
    }

    public Category getCategory() {
        return category;
    }

    public String getAddress() {
        return address;
    }

    public String getGu() {
        return gu;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public String getFee() {
        return fee;
    }

    public ReceptionStatus getReceptionStatus() {
        return receptionStatus;
    }

    public LocalDateTime getReceptionEndAt() {
        return receptionEndAt;
    }

    public ReservationType getReservationType() {
        return reservationType;
    }

    public String getReservationUrl() {
        return reservationUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public LocalDateTime getExternalUpdatedAt() {
        return externalUpdatedAt;
    }

    public LocalDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public RentalItem getRentalItem() {
        return rentalItem;
    }
}