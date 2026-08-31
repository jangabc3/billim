package com.billim.repository;

import com.billim.domain.item.RentalItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentalItemRepository extends JpaRepository<RentalItem, Long> {
}