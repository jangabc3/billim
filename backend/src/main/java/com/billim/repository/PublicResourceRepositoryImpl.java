package com.billim.repository;

import com.billim.common.Times;
import com.billim.domain.item.Category;
import com.billim.domain.resource.PublicResource;
import com.billim.domain.resource.QPublicResource;
import com.billim.domain.resource.ReceptionStatus;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PublicResourceRepositoryCustom의 QueryDSL 구현체.
 * 스프링 데이터 JPA 규약상 이름이 "Repository인터페이스명 + Impl"이어야
 * PublicResourceRepository가 이 구현을 자동으로 인식한다.
 *
 * 접수 상태 컬럼은 하루 한 번 동기화 때만 갱신되므로, 마감 시각(receptionEndAt)을 함께 비교해
 * "이미 끝났는데 OPEN으로 남아 있는" 항목이 결과에 섞이지 않게 한다.
 */
public class PublicResourceRepositoryImpl implements PublicResourceRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public PublicResourceRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<PublicResource> search(Category category, String gu,
            ReceptionStatus receptionStatus, String keyword, Boolean freeOnly,
            Integer closingWithinDays, Pageable pageable) {

        QPublicResource r = QPublicResource.publicResource;
        LocalDateTime now = Times.now();

        BooleanBuilder condition = new BooleanBuilder();
        if (category != null) {
            condition.and(r.category.eq(category));
        }
        if (gu != null && !gu.isBlank()) {
            condition.and(r.gu.eq(gu));
        }

        boolean closingSoonMode = closingWithinDays != null;
        if (closingSoonMode) {
            // N일 안에 끝나는 접수 — 이미 끝난 것과 확실히 마감(CLOSED)된 것은 제외
            condition.and(r.receptionStatus.ne(ReceptionStatus.CLOSED));
            condition.and(r.receptionEndAt.goe(now));
            condition.and(r.receptionEndAt.lt(now.plusDays(closingWithinDays)));
        } else if (receptionStatus == ReceptionStatus.OPEN) {
            // "이용 가능한 것"을 요청한 것으로 해석 — 마감됐거나 마감 시각이 지난 것만 제외한다.
            // 공유누리(UNKNOWN)처럼 접수 정보가 없는 것은 계속 노출한다.
            condition.and(r.receptionStatus.ne(ReceptionStatus.CLOSED));
            condition.and(r.receptionEndAt.isNull().or(r.receptionEndAt.goe(now)));
        } else if (receptionStatus == ReceptionStatus.CLOSING_SOON) {
            condition.and(r.receptionStatus.ne(ReceptionStatus.CLOSED));
            condition.and(r.receptionEndAt.goe(now));
            condition.and(r.receptionEndAt.lt(now.plusDays(1)));
        } else if (receptionStatus == ReceptionStatus.CLOSED) {
            condition.and(r.receptionStatus.eq(ReceptionStatus.CLOSED)
                    .or(r.receptionEndAt.lt(now)));
        } else if (receptionStatus != null) { // UNKNOWN
            condition.and(r.receptionStatus.eq(receptionStatus));
            condition.and(r.receptionEndAt.isNull().or(r.receptionEndAt.goe(now)));
        }

        if (keyword != null && !keyword.isBlank()) {
            condition.and(r.name.containsIgnoreCase(keyword));
        }
        if (Boolean.TRUE.equals(freeOnly)) {
            condition.and(r.fee.eq("무료"));
        }

        // 마감 임박 모드는 마감이 가까운 순, 그 외에는 최근 등록 순. id를 보조 정렬로 둬서 페이지 경계가 흔들리지 않게 한다.
        OrderSpecifier<?>[] orders = closingSoonMode
                ? new OrderSpecifier<?>[] { r.receptionEndAt.asc(), r.id.asc() }
                : new OrderSpecifier<?>[] { r.createdAt.desc(), r.id.desc() };

        List<PublicResource> content = queryFactory
                .selectFrom(r)
                .where(condition)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(orders)
                .fetch();

        Long total = queryFactory
                .select(r.count())
                .from(r)
                .where(condition)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
}