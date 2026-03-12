package com.medical.system.service.impl;

import com.medical.system.entity.BudgetExecution;
import com.medical.system.entity.BudgetPlan;
import com.medical.system.exception.BusinessException;
import com.medical.system.repository.BudgetExecutionRepository;
import com.medical.system.repository.BudgetPlanRepository;
import com.medical.system.repository.DepartmentRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetServiceImpl {

    private final BudgetPlanRepository budgetPlanRepository;
    private final BudgetExecutionRepository budgetExecutionRepository;
    private final DepartmentRepository departmentRepository;

    @Data
    public static class BudgetPlanVO {
        private Long id;
        private Long deptId;
        private String deptName;
        private Integer year;
        private Integer quarter;
        private String periodLabel; // "2026年度" / "2026年Q1"
        private String category;
        private BigDecimal budgetAmount;
        private BigDecimal usedAmount;
        private BigDecimal remainingAmount;
        private Double usageRate; // 0-100
        private String status;
        private String remark;
        private LocalDateTime createTime;
    }

    @Data
    public static class CreateBudgetRequest {
        private Long deptId;
        private Integer year;
        private Integer quarter;
        private String category;
        private BigDecimal budgetAmount;
        private String remark;
    }

    @Data
    public static class BudgetSummaryVO {
        private BigDecimal totalBudget;
        private BigDecimal totalUsed;
        private BigDecimal totalRemaining;
        private Double overallUsageRate;
        private int totalPlans;
        private int overBudgetPlans;
    }

    public List<BudgetPlanVO> getPlans(Integer year, Long deptId) {
        List<BudgetPlan> plans;
        if (deptId != null) {
            plans = budgetPlanRepository.findByDeptIdAndYearOrderByQuarterAsc(deptId, year);
        } else {
            plans = budgetPlanRepository.findByYearAndStatusOrderByDeptIdAsc(year, "ACTIVE");
        }

        // 批量加载科室名称
        List<Long> deptIds = plans.stream().map(BudgetPlan::getDeptId).distinct().collect(Collectors.toList());
        Map<Long, String> deptNames = departmentRepository.findAllById(deptIds).stream()
                .collect(Collectors.toMap(d -> d.getId(), d -> d.getDeptName()));

        return plans.stream().map(p -> toVO(p, deptNames.get(p.getDeptId()))).collect(Collectors.toList());
    }

    public BudgetSummaryVO getSummary(Integer year) {
        List<BudgetPlan> plans = budgetPlanRepository.findByYearAndStatusOrderByDeptIdAsc(year, "ACTIVE");
        BudgetSummaryVO summary = new BudgetSummaryVO();
        BigDecimal totalBudget = plans.stream().map(BudgetPlan::getBudgetAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalUsed = plans.stream().map(p -> p.getUsedAmount() != null ? p.getUsedAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalBudget(totalBudget);
        summary.setTotalUsed(totalUsed);
        summary.setTotalRemaining(totalBudget.subtract(totalUsed));
        summary.setOverallUsageRate(totalBudget.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                : totalUsed.divide(totalBudget, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue());
        summary.setTotalPlans(plans.size());
        summary.setOverBudgetPlans((int) plans.stream()
                .filter(p -> p.getUsedAmount() != null && p.getUsedAmount().compareTo(p.getBudgetAmount()) > 0)
                .count());
        return summary;
    }

    public List<BudgetExecution> getExecutions(Long planId) {
        return budgetExecutionRepository.findByPlanIdOrderByCreateTimeDesc(planId);
    }

    @Transactional
    public BudgetPlanVO createPlan(CreateBudgetRequest req, Long userId) {
        // 检查是否已存在同科室同期预算
        budgetPlanRepository.findExisting(req.getDeptId(), req.getYear(), req.getQuarter(), req.getCategory())
                .ifPresent(p -> { throw new BusinessException("该科室同时期预算已存在，请先修改现有预算"); });

        BudgetPlan plan = new BudgetPlan();
        plan.setDeptId(req.getDeptId());
        plan.setYear(req.getYear());
        plan.setQuarter(req.getQuarter());
        plan.setCategory(req.getCategory());
        plan.setBudgetAmount(req.getBudgetAmount());
        plan.setUsedAmount(BigDecimal.ZERO);
        plan.setStatus("ACTIVE");
        plan.setRemark(req.getRemark());
        plan.setCreatedBy(userId);
        BudgetPlan saved = budgetPlanRepository.save(plan);

        String deptName = departmentRepository.findById(req.getDeptId())
                .map(d -> d.getDeptName()).orElse("");
        return toVO(saved, deptName);
    }

    @Transactional
    public BudgetPlanVO updatePlan(Long id, BigDecimal budgetAmount, String remark) {
        BudgetPlan plan = budgetPlanRepository.findById(id)
                .orElseThrow(() -> new BusinessException("预算计划不存在"));
        plan.setBudgetAmount(budgetAmount);
        plan.setRemark(remark);
        budgetPlanRepository.save(plan);

        String deptName = departmentRepository.findById(plan.getDeptId())
                .map(d -> d.getDeptName()).orElse("");
        return toVO(plan, deptName);
    }

    /** 消耗预算（申领完成时调用） */
    @Transactional
    public void consumeBudget(Long deptId, Integer year, BigDecimal amount, Long requisitionId, String description) {
        // 找当前年度最匹配的预算计划（优先精确匹配季度，其次年度预算）
        List<BudgetPlan> plans = budgetPlanRepository.findByDeptIdAndYearOrderByQuarterAsc(deptId, year)
                .stream().filter(p -> "ACTIVE".equals(p.getStatus())).collect(Collectors.toList());
        if (plans.isEmpty()) return; // 无预算计划则不记录

        BudgetPlan plan = plans.stream().filter(p -> p.getQuarter() == null).findFirst()
                .orElse(plans.get(0));

        plan.setUsedAmount(plan.getUsedAmount() == null
                ? amount : plan.getUsedAmount().add(amount));
        budgetPlanRepository.save(plan);

        BudgetExecution exec = new BudgetExecution();
        exec.setPlanId(plan.getId());
        exec.setDeptId(deptId);
        exec.setRequisitionId(requisitionId);
        exec.setAmount(amount);
        exec.setDescription(description);
        budgetExecutionRepository.save(exec);
    }

    private BudgetPlanVO toVO(BudgetPlan p, String deptName) {
        BudgetPlanVO vo = new BudgetPlanVO();
        vo.setId(p.getId());
        vo.setDeptId(p.getDeptId());
        vo.setDeptName(deptName);
        vo.setYear(p.getYear());
        vo.setQuarter(p.getQuarter());
        vo.setPeriodLabel(p.getQuarter() == null ? p.getYear() + "年度" : p.getYear() + "年Q" + p.getQuarter());
        vo.setCategory(p.getCategory());
        vo.setBudgetAmount(p.getBudgetAmount());
        BigDecimal used = p.getUsedAmount() != null ? p.getUsedAmount() : BigDecimal.ZERO;
        vo.setUsedAmount(used);
        vo.setRemainingAmount(p.getBudgetAmount().subtract(used));
        vo.setUsageRate(p.getBudgetAmount().compareTo(BigDecimal.ZERO) == 0 ? 0.0
                : used.divide(p.getBudgetAmount(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue());
        vo.setStatus(p.getStatus());
        vo.setRemark(p.getRemark());
        vo.setCreateTime(p.getCreateTime());
        return vo;
    }
}
