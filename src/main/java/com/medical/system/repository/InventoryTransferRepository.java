package com.medical.system.repository;

import com.medical.system.entity.InventoryTransfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryTransferRepository extends JpaRepository<InventoryTransfer, Long> {
    @Query("SELECT t FROM InventoryTransfer t WHERE :keyword IS NULL OR t.transferNo LIKE %:keyword% OR t.toLocation LIKE %:keyword%")
    Page<InventoryTransfer> findByConditions(@Param("keyword") String keyword, Pageable pageable);
}
