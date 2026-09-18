package com.billim.repository;

import com.billim.domain.sync.SyncCursor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncCursorRepository extends JpaRepository<SyncCursor, String> {
}