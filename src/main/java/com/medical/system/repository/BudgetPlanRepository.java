package com.medical.system.repository;

import com.medical.system.entity.BudgetPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BudgetPlanRepository extends JpaRepository<BudgetPlan, Long> {

    List<BudgetPlan> findByYearAndStatusOrderByDeptIdAsc(Integer year, String status);

    List<BudgetPlan> findByDeptIdAndYearOrderByQuarterAsc(Long deptId, Integer year);

    @Query(value = "SELECT * FROM budget_plans b WHERE b.dept_id = :deptId AND b.year = :year " +
           "AND ((:quarter IS NULL AND b.quarter IS NULL) OR b.quarter = :quarter) " +
           "AND ((:category IS NULL AND b.category IS NULL) OR b.category = :category) LIMIT 1",
           nativeQuery = true)
    Optional<BudgetPlan> findExisting(@Param("deptId") Long deptId, @Param("year") Integer year,
                                      @Param("quarter") Integer quarter, @Param("category") String category);
}
