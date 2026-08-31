package com.billim.repository;

import com.billim.domain.item.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByRentalItemId(Long rentalItemId);
}