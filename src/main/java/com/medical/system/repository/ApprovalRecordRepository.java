package com.medical.system.repository;

import com.medical.system.entity.ApprovalRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalRecordRepository extends JpaRepository<ApprovalRecord, Long> {
    List<ApprovalRecord> findByRequisitionIdOrderByApprovalTimeAsc(Long requisitionId);
}
