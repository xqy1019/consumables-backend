package com.medical.system.repository;

import com.medical.system.entity.PurchaseRequisition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseRequisitionRepository extends JpaRepository<PurchaseRequisition, Long> {
    @Query("SELECT r FROM PurchaseRequisition r WHERE (:keyword IS NULL OR r.reqNo LIKE %:keyword%) AND (:status IS NULL OR r.status = :status) AND (:deptId IS NULL OR r.deptId = :deptId)")
    Page<PurchaseRequisition> findByConditions(@Param("keyword") String keyword, @Param("status") String status, @Param("deptId") Long deptId, Pageable pageable);

    java.util.List<PurchaseRequisition> findByStatus(String status);
}
