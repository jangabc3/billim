package com.billim.service.sync;

import com.billim.domain.sync.SyncLog;
import com.billim.repository.SyncLogRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * SyncLog 저장을 별도 트랜잭션으로 분리한다. 동기화 자체가 실패해서 호출한 쪽의
 * 
 * @Transactional 메서드가 롤백되더라도 "실패했다"는 로그는 남아야 의미가 있기
 *                때문에, REQUIRES_NEW로 독립된 트랜잭션에서 커밋한다. 같은 클래스 안에서
 *                self-invocation으로 호출하면 프록시를 우회해 트랜잭션 전파가 안 먹기 때문에
 *                별도 빈으로 분리했다 — Reservation 동시성 처리, resilience4j 적용 때와 동일한 이유.
 */
@Component
public class SyncLogRecorder {

    private final SyncLogRepository syncLogRepository;

    public SyncLogRecorder(SyncLogRepository syncLogRepository) {
        this.syncLogRepository = syncLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(SyncLog syncLog) {
        syncLogRepository.save(syncLog);
    }
}