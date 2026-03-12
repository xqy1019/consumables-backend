package com.medical.system.repository;

import com.medical.system.entity.Requisition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RequisitionRepository extends JpaRepository<Requisition, Long> {

    @Query("SELECT r FROM Requisition r WHERE " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:deptId IS NULL OR r.deptId = :deptId) AND " +
           "(:createdBy IS NULL OR r.createdBy = :createdBy)")
    Page<Requisition> findByConditions(@Param("status") String status,
                                       @Param("deptId") Long deptId,
                                       @Param("createdBy") Long createdBy,
                                       Pageable pageable);

    long countByStatus(String status);

    List<Requisition> findByStatus(String status);

    @Query("SELECT r FROM Requisition r WHERE r.createTime >= :since ORDER BY r.createTime DESC")
    List<Requisition> findRecentRequisitions(@Param("since") LocalDateTime since, Pageable pageable);

    /** 从数据库序列获取下一个序号（支持多实例部署，重启不重置） */
    @Query(value = "SELECT nextval('requisition_seq')", nativeQuery = true)
    Long nextRequisitionSeq();
}
