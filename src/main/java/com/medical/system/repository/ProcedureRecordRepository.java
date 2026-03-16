package com.medical.system.repository;

import com.medical.system.entity.ProcedureRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ProcedureRecordRepository extends JpaRepository<ProcedureRecord, Long> {

    List<ProcedureRecord> findByDeptIdOrderByPerformedAtDesc(Long deptId);

    List<ProcedureRecord> findByDeptIdAndPerformedAtBetweenOrderByPerformedAtDesc(
            Long deptId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT pr FROM ProcedureRecord pr WHERE pr.performedAt BETWEEN :start AND :end ORDER BY pr.performedAt DESC")
    List<ProcedureRecord> findByPerformedAtBetween(LocalDateTime start, LocalDateTime end);
}
