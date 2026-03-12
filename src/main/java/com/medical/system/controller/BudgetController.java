package com.medical.system.controller;

import com.medical.system.annotation.Log;
import com.medical.system.common.Result;
import com.medical.system.entity.BudgetExecution;
import com.medical.system.security.SecurityUtils;
import com.medical.system.service.impl.BudgetServiceImpl;
import com.medical.system.service.impl.BudgetServiceImpl.BudgetPlanVO;
import com.medical.system.service.impl.BudgetServiceImpl.BudgetSummaryVO;
import com.medical.system.service.impl.BudgetServiceImpl.CreateBudgetRequest;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/budget")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetServiceImpl budgetService;

    @GetMapping
    @PreAuthorize("hasAuthority('menu:inventory') or hasAuthority('menu:report')")
    public Result<List<BudgetPlanVO>> getPlans(
            @RequestParam(defaultValue = "2026") Integer year,
            @RequestParam(required = false) Long deptId) {
        return Result.success(budgetService.getPlans(year, deptId));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('menu:inventory') or hasAuthority('menu:report')")
    public Result<BudgetSummaryVO> getSummary(
            @RequestParam(defaultValue = "2026") Integer year) {
        return Result.success(budgetService.getSummary(year));
    }

    @GetMapping("/{id}/executions")
    @PreAuthorize("hasAuthority('menu:inventory') or hasAuthority('menu:report')")
    public Result<List<BudgetExecution>> getExecutions(@PathVariable Long id) {
        return Result.success(budgetService.getExecutions(id));
    }

    @Log(module = "预算管理", action = "新建预算")
    @PostMapping
    @PreAuthorize("hasAuthority('inventory:edit')")
    public Result<BudgetPlanVO> createPlan(@Valid @RequestBody CreateBudgetRequest request) {
        return Result.success(budgetService.createPlan(request, SecurityUtils.getCurrentUserId()));
    }

    @Data
    static class UpdateBudgetRequest {
        private BigDecimal budgetAmount;
        private String remark;
    }

    @Log(module = "预算管理", action = "修改预算")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:edit')")
    public Result<BudgetPlanVO> updatePlan(@PathVariable Long id,
                                           @RequestBody UpdateBudgetRequest request) {
        return Result.success(budgetService.updatePlan(id, request.getBudgetAmount(), request.getRemark()));
    }
}
