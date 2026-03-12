package com.medical.system.repository;

import com.medical.system.entity.PurchaseInquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseInquiryRepository extends JpaRepository<PurchaseInquiry, Long> {
    @Query("SELECT i FROM PurchaseInquiry i WHERE (:keyword IS NULL OR i.inquiryNo LIKE %:keyword%) AND (:status IS NULL OR i.status = :status)")
    Page<PurchaseInquiry> findByConditions(@Param("keyword") String keyword, @Param("status") String status, Pageable pageable);
}
