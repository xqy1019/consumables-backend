package com.medical.system.repository;

import com.medical.system.entity.BudgetExecution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BudgetExecutionRepository extends JpaRepository<BudgetExecution, Long> {
    List<BudgetExecution> findByPlanIdOrderByCreateTimeDesc(Long planId);
    List<BudgetExecution> findByDeptIdOrderByCreateTimeDesc(Long deptId);
}
