package com.medical.system.repository;

import com.medical.system.entity.InventoryStocktakingItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryStocktakingItemRepository extends JpaRepository<InventoryStocktakingItem, Long> {
    List<InventoryStocktakingItem> findByStocktakingId(Long stocktakingId);
    void deleteByStocktakingId(Long stocktakingId);
}
