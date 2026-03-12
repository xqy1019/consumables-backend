package com.medical.system.repository;

import com.medical.system.entity.InventoryDamage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryDamageRepository extends JpaRepository<InventoryDamage, Long> {
    @Query("SELECT d FROM InventoryDamage d WHERE :keyword IS NULL OR d.damageNo LIKE %:keyword%")
    Page<InventoryDamage> findByConditions(@Param("keyword") String keyword, Pageable pageable);
}
