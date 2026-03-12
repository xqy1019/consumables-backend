package com.medical.system.repository;

import com.medical.system.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    boolean existsByDeptCode(String deptCode);
    List<Department> findByStatus(Integer status);
    List<Department> findByParentId(Long parentId);
    List<Department> findByStatusOrderByLevelAscDeptNameAsc(Integer status);
}
