package com.medical.system.repository;

import com.medical.system.entity.DeptStocktakingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DeptStocktakingItemRepository extends JpaRepository<DeptStocktakingItem, Long> {

    List<DeptStocktakingItem> findByStocktakingId(Long stocktakingId);

    List<DeptStocktakingItem> findByStocktakingIdIn(List<Long> stocktakingIds);
}
