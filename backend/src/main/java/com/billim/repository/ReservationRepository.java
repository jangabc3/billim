package com.billim.repository;

import com.billim.domain.reservation.Reservation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByUserId(Long userId);

    /**
     * 내 예약 목록 — 응답에 물품 이름이 필요하므로 rentalItem을 한 번의 쿼리로 함께 가져온다.
     * (지연 로딩에 맡기면 트랜잭션 밖에서 LazyInitializationException, 트랜잭션 안이어도 예약 수만큼 N+1 쿼리 발생)
     */
    @EntityGraph(attributePaths = "rentalItem")
    List<Reservation> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** 단건 조회 응답용 — 물품 이름까지 함께 가져온다. */
    @EntityGraph(attributePaths = "rentalItem")
    Optional<Reservation> findWithRentalItemById(Long id);

    /** 기관 권한 확인용 — 물품과 그 소속 기관까지 함께 가져온다. */
    @EntityGraph(attributePaths = { "rentalItem", "rentalItem.institution" })
    Optional<Reservation> findWithRentalItemAndInstitutionById(Long id);
}