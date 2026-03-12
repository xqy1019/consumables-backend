package com.medical.system.repository;

import com.medical.system.entity.PurchaseRequisitionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseRequisitionItemRepository extends JpaRepository<PurchaseRequisitionItem, Long> {
    List<PurchaseRequisitionItem> findByReqId(Long reqId);
    void deleteByReqId(Long reqId);
}
