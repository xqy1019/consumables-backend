package com.medical.system.repository;

import com.medical.system.entity.PurchaseContract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseContractRepository extends JpaRepository<PurchaseContract, Long> {
    @Query("SELECT c FROM PurchaseContract c WHERE (:keyword IS NULL OR c.contractNo LIKE %:keyword%) AND (:status IS NULL OR c.status = :status)")
    Page<PurchaseContract> findByConditions(@Param("keyword") String keyword, @Param("status") String status, Pageable pageable);
}
