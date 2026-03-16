package com.medical.system.repository;

import com.medical.system.entity.AnomalyWorkOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface AnomalyWorkOrderRepository extends JpaRepository<AnomalyWorkOrder, Long>,
        JpaSpecificationExecutor<AnomalyWorkOrder> {
    List<AnomalyWorkOrder> findByStatus(String status);
    List<AnomalyWorkOrder> findByDeptIdAndStatus(Long deptId, String status);
    List<AnomalyWorkOrder> findByDeptId(Long deptId);
    List<AnomalyWorkOrder> findByAssignedTo(Long userId);
    List<AnomalyWorkOrder> findAllByOrderByCreatedAtDesc();
    Page<AnomalyWorkOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<AnomalyWorkOrder> findByDeptIdOrderByCreatedAtDesc(Long deptId, Pageable pageable);
    long countByStatus(String status);

    List<AnomalyWorkOrder> findByDeptIdAndMaterialIdAndStatusNot(Long deptId, Long materialId, String status);

    List<AnomalyWorkOrder> findByStatusIn(List<String> statuses);

    List<AnomalyWorkOrder> findBySlaBreachedTrueAndStatusIn(List<String> statuses);
}
