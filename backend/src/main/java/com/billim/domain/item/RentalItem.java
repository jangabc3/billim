package com.billim.domain.item;

import com.billim.domain.institution.Institution;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rental_items")
public class RentalItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @Column(nullable = false, length = 60)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @Column(length = 500)
    private String description;

    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FeeType feeType;

    private Integer feeAmount; // feeType == PAID일 때만 사용

    @Column(nullable = false)
    private Integer maxRentalDays;

    @Column(nullable = false)
    private boolean requiresId; // 신분증 필요 여부

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected RentalItem() {
    }

    public RentalItem(Institution institution, String name, Category category,
                       FeeType feeType, Integer feeAmount, Integer maxRentalDays, boolean requiresId) {
        this.institution = institution;
        this.name = name;
        this.category = category;
        this.feeType = feeType;
        this.feeAmount = feeAmount;
        this.maxRentalDays = maxRentalDays;
        this.requiresId = requiresId;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Institution getInstitution() { return institution; }
    public String getName() { return name; }
    public Category getCategory() { return category; }
}
