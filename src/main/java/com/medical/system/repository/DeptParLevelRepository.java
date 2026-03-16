package com.medical.system.repository;

import com.medical.system.entity.DeptParLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeptParLevelRepository extends JpaRepository<DeptParLevel, Long> {

    List<DeptParLevel> findByDeptIdAndIsActiveTrue(Long deptId);

    List<DeptParLevel> findByIsActiveTrue();

    Optional<DeptParLevel> findByDeptIdAndMaterialId(Long deptId, Long materialId);

    List<DeptParLevel> findByMaterialId(Long materialId);
}
