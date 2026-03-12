package com.medical.system.repository;

import com.medical.system.entity.PurchaseInquiryItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PurchaseInquiryItemRepository extends JpaRepository<PurchaseInquiryItem, Long> {
    List<PurchaseInquiryItem> findByInquiryId(Long inquiryId);

    @Query("SELECT pii.quotedPrice FROM PurchaseInquiryItem pii " +
           "JOIN PurchaseInquiry pi ON pi.id = pii.inquiryId " +
           "WHERE pii.materialId = :materialId AND pi.status = 'CONFIRMED' " +
           "ORDER BY pi.inquiryDate DESC")
    List<BigDecimal> findHistoricalPrices(@Param("materialId") Long materialId, Pageable pageable);
}
