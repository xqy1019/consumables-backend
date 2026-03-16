package com.medical.system.repository;

import com.medical.system.entity.DeptInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DeptInventoryRepository extends JpaRepository<DeptInventory, Long> {

    List<DeptInventory> findByDeptId(Long deptId);

    Optional<DeptInventory> findByDeptIdAndMaterialId(Long deptId, Long materialId);

    List<DeptInventory> findByDeptIdIn(List<Long> deptIds);
}
