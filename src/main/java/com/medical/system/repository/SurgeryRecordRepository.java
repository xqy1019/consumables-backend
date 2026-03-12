package com.medical.system.repository;

import com.medical.system.entity.SurgeryRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SurgeryRecordRepository extends JpaRepository<SurgeryRecord, Long> {
    List<SurgeryRecord> findByPatientId(String patientId);

    @Query("SELECT s FROM SurgeryRecord s WHERE (:keyword IS NULL OR s.patientName LIKE %:keyword% OR s.surgeryNo LIKE %:keyword% OR s.doctorName LIKE %:keyword%) AND (:deptId IS NULL OR s.deptId = :deptId)")
    Page<SurgeryRecord> findByConditions(@Param("keyword") String keyword, @Param("deptId") Long deptId, Pageable pageable);
}
