package com.billim.repository;

import com.billim.domain.sync.SyncLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncLogRepository extends JpaRepository<SyncLog, Long> {
}