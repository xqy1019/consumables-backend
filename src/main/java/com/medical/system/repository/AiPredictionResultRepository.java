package com.medical.system.repository;

import com.medical.system.entity.AiPredictionResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AiPredictionResultRepository extends JpaRepository<AiPredictionResult, Long> {
    Optional<AiPredictionResult> findByMaterialIdAndDeptIdAndPredictionMonth(Long materialId, Long deptId, String predictionMonth);

    List<AiPredictionResult> findByPredictionMonthOrderByMaterialIdAsc(String predictionMonth);

    @Query("SELECT p FROM AiPredictionResult p WHERE (:month IS NULL OR p.predictionMonth = :month)")
    Page<AiPredictionResult> findByConditions(@Param("month") String month, Pageable pageable);

    @Query("SELECT p FROM AiPredictionResult p WHERE p.predictionMonth = :month AND p.actualQuantity IS NOT NULL")
    List<AiPredictionResult> findWithActualByMonth(@Param("month") String month);

    List<AiPredictionResult> findByPredictionMonth(String predictionMonth);
}
