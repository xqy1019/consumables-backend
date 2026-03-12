package com.medical.system.repository;

import com.medical.system.entity.ReturnRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    @Query("SELECT r FROM ReturnRequest r WHERE " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:deptId IS NULL OR r.deptId = :deptId)")
    Page<ReturnRequest> findByConditions(@Param("status") String status,
                                         @Param("deptId") Long deptId,
                                         Pageable pageable);
}
