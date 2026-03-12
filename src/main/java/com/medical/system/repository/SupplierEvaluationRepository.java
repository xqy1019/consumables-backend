package com.medical.system.repository;

import com.medical.system.entity.SupplierEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierEvaluationRepository extends JpaRepository<SupplierEvaluation, Long> {

    List<SupplierEvaluation> findBySupplierIdOrderByEvalYearDescEvalQuarterDesc(Long supplierId);

    List<SupplierEvaluation> findByEvalYearAndEvalQuarterOrderByTotalScoreDesc(Integer year, Integer quarter);

    Optional<SupplierEvaluation> findBySupplierIdAndEvalYearAndEvalQuarter(Long supplierId, Integer year, Integer quarter);
}
