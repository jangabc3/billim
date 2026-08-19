package com.billim.domain.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password; // BCrypt 해시 저장

    @Column(nullable = false, length = 30)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    // INSTITUTION_ADMIN인 경우에만 값이 있음. 자신이 담당하는 기관만 수정 가능하도록 서비스단에서 검증.
    @Column(name = "managed_institution_id")
    private Long managedInstitutionId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected User() {
    }

    public User(String email, String password, String name, UserRole role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public UserRole getRole() { return role; }
    public Long getManagedInstitutionId() { return managedInstitutionId; }

    public void assignInstitution(Long institutionId) {
        this.managedInstitutionId = institutionId;
    }

    // 기관 관리자가 자신이 담당하는 기관만 수정할 수 있는지 검증할 때 사용
    public boolean canManage(Long institutionId) {
        return this.role == UserRole.SYSTEM_ADMIN
                || (this.role == UserRole.INSTITUTION_ADMIN && institutionId.equals(this.managedInstitutionId));
    }
}
