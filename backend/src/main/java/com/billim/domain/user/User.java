package com.billim.domain.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_MINUTES = 15;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 30)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(name = "managed_institution_id")
    private Long managedInstitutionId;

    @Column(nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

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

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public UserRole getRole() {
        return role;
    }

    public Long getManagedInstitutionId() {
        return managedInstitutionId;
    }

    public void assignInstitution(Long institutionId) {
        this.managedInstitutionId = institutionId;
    }

    public boolean canManage(Long institutionId) {
        return this.role == UserRole.SYSTEM_ADMIN
                || (this.role == UserRole.INSTITUTION_ADMIN && institutionId.equals(this.managedInstitutionId));
    }

    /** 현재 잠금 상태인지 확인한다. 잠금 시각이 지났으면 자동으로 풀린 것으로 간주한다. */
    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    /** 로그인 실패를 기록한다. 연속 실패가 기준치를 넘으면 일정 시간 계정을 잠근다. */
    public void recordLoginFailure() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= MAX_FAILED_ATTEMPTS) {
            this.lockedUntil = LocalDateTime.now().plusMinutes(LOCK_MINUTES);
        }
    }

    /** 로그인 성공 시 실패 기록을 초기화한다. */
    public void recordLoginSuccess() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }
}