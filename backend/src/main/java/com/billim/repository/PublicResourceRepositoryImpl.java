package com.billim.repository;

import com.billim.domain.item.Category;
import com.billim.domain.resource.PublicResource;
import com.billim.domain.resource.QPublicResource;
import com.billim.domain.resource.ReceptionStatus;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * PublicResourceRepositoryCustom의 QueryDSL 구현체.
 * 스프링 데이터 JPA 규약상 이름이 "Repository인터페이스명 + Impl"이어야
 * PublicResourceRepository가 이 구현을 자동으로 인식한다.
 */
public class PublicResourceRepositoryImpl implements PublicResourceRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public PublicResourceRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<PublicResource> search(Category category, String gu,
            ReceptionStatus receptionStatus, String keyword, Pageable pageable) {

        QPublicResource r = QPublicResource.publicResource;

        BooleanBuilder condition = new BooleanBuilder();
        if (category != null) {
            condition.and(r.category.eq(category));
        }
        if (gu != null && !gu.isBlank()) {
            condition.and(r.gu.eq(gu));
        }
        if (receptionStatus != null) {
            condition.and(r.receptionStatus.eq(receptionStatus));
        }
        if (keyword != null && !keyword.isBlank()) {
            condition.and(r.name.containsIgnoreCase(keyword));
        }

        List<PublicResource> content = queryFactory
                .selectFrom(r)
                .where(condition)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(r.createdAt.desc())
                .fetch();

        Long total = queryFactory
                .select(r.count())
                .from(r)
                .where(condition)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
}