package com.medical.system.repository;

import com.medical.system.entity.RequisitionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequisitionItemRepository extends JpaRepository<RequisitionItem, Long> {
    List<RequisitionItem> findByRequisitionId(Long requisitionId);
    List<RequisitionItem> findByRequisitionIdIn(List<Long> requisitionIds);
}
