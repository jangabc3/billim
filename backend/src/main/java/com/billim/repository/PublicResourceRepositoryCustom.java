package com.billim.repository;

import com.billim.domain.item.Category;
import com.billim.domain.resource.PublicResource;
import com.billim.domain.resource.ReceptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * QueryDSL로 짜는 동적 검색 쿼리 전용 인터페이스.
 * 반경검색(ST_DWithin)은 PostGIS 특수 함수라 QueryDSL 표준 지원이 마땅치 않아
 * PublicResourceRepository에 네이티브 쿼리로 별도 분리했다 — 이 인터페이스는
 * category/gu/receptionStatus/keyword처럼 일반적인 조건 조합 검색만 담당한다.
 */
public interface PublicResourceRepositoryCustom {

    Page<PublicResource> search(Category category, String gu,
            ReceptionStatus receptionStatus, String keyword, Pageable pageable);
}