package com.medical.system.repository;

import com.medical.system.entity.DeptStocktaking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface DeptStocktakingRepository extends JpaRepository<DeptStocktaking, Long> {

    List<DeptStocktaking> findByDeptIdOrderByCreatedAtDesc(Long deptId);

    List<DeptStocktaking> findByStatusOrderByCreatedAtDesc(String status);

    List<DeptStocktaking> findByStatusAndCompletedAtBetween(String status, LocalDateTime start, LocalDateTime end);

    List<DeptStocktaking> findByDeptIdAndStatusAndCompletedAtBetween(Long deptId, String status, LocalDateTime start, LocalDateTime end);
}
