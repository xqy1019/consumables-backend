package com.medical.system.service.impl;

import com.medical.system.entity.SupplierEvaluation;
import com.medical.system.exception.BusinessException;
import com.medical.system.repository.InventoryRepository;
import com.medical.system.repository.SupplierEvaluationRepository;
import com.medical.system.repository.SupplierRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierPerformanceServiceImpl {

    private final SupplierEvaluationRepository evalRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryRepository inventoryRepository;

    @Data
    public static class PerformanceVO {
        private Long id;
        private Long supplierId;
        private String supplierName;
        private Integer evalYear;
        private Integer evalQuarter;
        private String periodLabel;
        private BigDecimal priceScore;
        private BigDecimal qualityScore;
        private BigDecimal deliveryScore;
        private BigDecimal serviceScore;
        private BigDecimal totalScore;
        private String grade;
        private BigDecimal qualityRate;
        private BigDecimal deliveryRate;
        private BigDecimal avgPriceRatio;
        private String remark;
        private String createTime;
    }

    @Data
    public static class EvaluateRequest {
        private Long supplierId;
        private Integer evalYear;
        private Integer evalQuarter;
        private BigDecimal deliveryRate;  // 手动输入交货及时率
        private BigDecimal serviceScore;  // 手动输入服务评分
        private String remark;
    }

    public List<PerformanceVO> getRankings(Integer year, Integer quarter) {
        List<SupplierEvaluation> evals = evalRepository.findByEvalYearAndEvalQuarterOrderByTotalScoreDesc(year, quarter);
        List<Long> supplierIds = evals.stream().map(SupplierEvaluation::getSupplierId).distinct().collect(Collectors.toList());
        Map<Long, String> nameMap = supplierIds.isEmpty() ? Map.of() :
                supplierRepository.findAllById(supplierIds).stream()
                        .collect(Collectors.toMap(s -> s.getId(), s -> s.getSupplierName()));
        return evals.stream().map(e -> toVO(e, nameMap.get(e.getSupplierId()))).collect(Collectors.toList());
    }

    public List<PerformanceVO> getSupplierHistory(Long supplierId) {
        return evalRepository.findBySupplierIdOrderByEvalYearDescEvalQuarterDesc(supplierId)
                .stream()
                .map(e -> {
                    String name = supplierRepository.findById(e.getSupplierId())
                            .map(s -> s.getSupplierName()).orElse("");
                    return toVO(e, name);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public PerformanceVO evaluate(EvaluateRequest req, Long userId) {
        // 检查是否已评价
        evalRepository.findBySupplierIdAndEvalYearAndEvalQuarter(req.getSupplierId(), req.getEvalYear(), req.getEvalQuarter())
                .ifPresent(e -> { throw new BusinessException("该供应商此季度已完成评价，请勿重复提交"); });

        // 自动计算质量合格率（从 inventory 表统计）
        List<Long> ids = List.of(req.getSupplierId());
        List<Object[]> qualityData = inventoryRepository.findQualityRateBySupplierIds(ids);
        BigDecimal qualityRate = BigDecimal.valueOf(100);
        if (!qualityData.isEmpty()) {
            Object[] row = qualityData.get(0);
            long total = ((Number) row[1]).longValue();
            long passed = ((Number) row[2]).longValue();
            if (total > 0) {
                qualityRate = BigDecimal.valueOf(passed * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
            }
        }

        // 计算各维度得分
        BigDecimal qualityScore = qualityRate; // 合格率直接等于得分
        BigDecimal deliveryScore = req.getDeliveryRate() != null ? req.getDeliveryRate() : BigDecimal.valueOf(80);
        BigDecimal serviceScore = req.getServiceScore() != null
                ? req.getServiceScore().min(BigDecimal.valueOf(100)) : BigDecimal.valueOf(80);
        BigDecimal priceScore = BigDecimal.valueOf(80); // 默认，后续可接入采购价格比较

        // 加权综合得分：质量40% + 价格25% + 交期25% + 服务10%
        BigDecimal totalScore = qualityScore.multiply(BigDecimal.valueOf(0.40))
                .add(priceScore.multiply(BigDecimal.valueOf(0.25)))
                .add(deliveryScore.multiply(BigDecimal.valueOf(0.25)))
                .add(serviceScore.multiply(BigDecimal.valueOf(0.10)))
                .setScale(2, RoundingMode.HALF_UP);

        String grade;
        double score = totalScore.doubleValue();
        if (score >= 90) grade = "A";
        else if (score >= 75) grade = "B";
        else if (score >= 60) grade = "C";
        else grade = "D";

        SupplierEvaluation eval = new SupplierEvaluation();
        eval.setSupplierId(req.getSupplierId());
        eval.setEvalYear(req.getEvalYear());
        eval.setEvalQuarter(req.getEvalQuarter());
        eval.setPriceScore(priceScore);
        eval.setQualityScore(qualityScore);
        eval.setDeliveryScore(deliveryScore);
        eval.setServiceScore(serviceScore);
        eval.setTotalScore(totalScore);
        eval.setGrade(grade);
        eval.setQualityRate(qualityRate);
        eval.setDeliveryRate(req.getDeliveryRate());
        eval.setRemark(req.getRemark());
        eval.setEvaluatedBy(userId);

        SupplierEvaluation saved = evalRepository.save(eval);
        String name = supplierRepository.findById(req.getSupplierId()).map(s -> s.getSupplierName()).orElse("");
        return toVO(saved, name);
    }

    private PerformanceVO toVO(SupplierEvaluation e, String supplierName) {
        PerformanceVO vo = new PerformanceVO();
        vo.setId(e.getId());
        vo.setSupplierId(e.getSupplierId());
        vo.setSupplierName(supplierName);
        vo.setEvalYear(e.getEvalYear());
        vo.setEvalQuarter(e.getEvalQuarter());
        vo.setPeriodLabel(e.getEvalYear() + "年Q" + e.getEvalQuarter());
        vo.setPriceScore(e.getPriceScore());
        vo.setQualityScore(e.getQualityScore());
        vo.setDeliveryScore(e.getDeliveryScore());
        vo.setServiceScore(e.getServiceScore());
        vo.setTotalScore(e.getTotalScore());
        vo.setGrade(e.getGrade());
        vo.setQualityRate(e.getQualityRate());
        vo.setDeliveryRate(e.getDeliveryRate());
        vo.setAvgPriceRatio(e.getAvgPriceRatio());
        vo.setRemark(e.getRemark());
        vo.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        return vo;
    }
}
