package com.billim.domain.sync;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 공유누리 목록 API를 카테고리(rsrcClsCd)별로 어디까지 읽었는지 기록한다.
 * 전국 데이터가 많은 카테고리(예: 캠핑 17,300건)를 한 번에 다 읽으면 API 일일
 * 호출 한도를 초과하므로, 실행할 때마다 이어서 다음 구간을 읽는 증분 수집 방식을 쓴다.
 */
@Entity
@Table(name = "sync_cursors")
public class SyncCursor {

    @Id
    @Column(name = "rsrc_cls_cd", length = 10)
    private String rsrcClsCd;

    @Column(name = "last_page_no", nullable = false)
    private int lastPageNo;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected SyncCursor() {
    }

    public SyncCursor(String rsrcClsCd) {
        this.rsrcClsCd = rsrcClsCd;
        this.lastPageNo = 0;
        this.updatedAt = LocalDateTime.now();
    }

    /** 다음 배치를 읽기 전, 이번에 읽을 시작 페이지(마지막으로 읽은 페이지 + 1)를 반환한다. */
    public int nextStartPage() {
        return lastPageNo + 1;
    }

    /** 이번 배치를 다 읽은 뒤 마지막 페이지 번호를 갱신한다. */
    public void advance(int newLastPageNo) {
        this.lastPageNo = newLastPageNo;
        this.updatedAt = LocalDateTime.now();
    }

    /** 전체 데이터를 다 훑었을 때(더 이상 새 페이지가 없을 때) 처음부터 다시 돌게 리셋한다. */
    public void reset() {
        this.lastPageNo = 0;
        this.updatedAt = LocalDateTime.now();
    }

    public String getRsrcClsCd() {
        return rsrcClsCd;
    }

    public int getLastPageNo() {
        return lastPageNo;
    }
}