package com.billim.domain.resource;

import com.billim.domain.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "favorite_resources",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "public_resource_id"})
)
public class FavoriteResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "public_resource_id", nullable = false)
    private PublicResource publicResource;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected FavoriteResource() {
    }

    public FavoriteResource(User user, PublicResource publicResource) {
        this.user = user;
        this.publicResource = publicResource;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
