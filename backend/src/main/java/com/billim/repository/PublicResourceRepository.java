package com.billim.repository;

import com.billim.domain.resource.PublicResource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicResourceRepository extends JpaRepository<PublicResource, Long> {
    // 이후 QueryDSL로 category+gu+freeOnly+radius 동적 검색 메서드 추가 예정
}
