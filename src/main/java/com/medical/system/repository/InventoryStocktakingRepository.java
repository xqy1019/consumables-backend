package com.medical.system.repository;

import com.medical.system.entity.InventoryStocktaking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryStocktakingRepository extends JpaRepository<InventoryStocktaking, Long> {
    @Query("SELECT s FROM InventoryStocktaking s WHERE (:keyword IS NULL OR s.stocktakingNo LIKE %:keyword% OR s.location LIKE %:keyword%) AND (:status IS NULL OR s.status = :status)")
    Page<InventoryStocktaking> findByConditions(@Param("keyword") String keyword, @Param("status") String status, Pageable pageable);
}
