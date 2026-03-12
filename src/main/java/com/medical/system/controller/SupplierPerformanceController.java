package com.medical.system.controller;

import com.medical.system.annotation.Log;
import com.medical.system.common.Result;
import com.medical.system.security.SecurityUtils;
import com.medical.system.service.impl.SupplierPerformanceServiceImpl;
import com.medical.system.service.impl.SupplierPerformanceServiceImpl.EvaluateRequest;
import com.medical.system.service.impl.SupplierPerformanceServiceImpl.PerformanceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierPerformanceController {

    private final SupplierPerformanceServiceImpl performanceService;

    @GetMapping("/performance/rankings")
    @PreAuthorize("hasAuthority('menu:supplier') or hasAuthority('menu:purchase')")
    public Result<List<PerformanceVO>> getRankings(
            @RequestParam(defaultValue = "2026") Integer year,
            @RequestParam(defaultValue = "1") Integer quarter) {
        return Result.success(performanceService.getRankings(year, quarter));
    }

    @GetMapping("/{id}/performance")
    @PreAuthorize("hasAuthority('menu:supplier') or hasAuthority('menu:purchase')")
    public Result<List<PerformanceVO>> getSupplierHistory(@PathVariable Long id) {
        return Result.success(performanceService.getSupplierHistory(id));
    }

    @Log(module = "供应商管理", action = "绩效评价")
    @PostMapping("/performance/evaluate")
    @PreAuthorize("hasAuthority('menu:supplier') or hasAuthority('purchase:edit')")
    public Result<PerformanceVO> evaluate(@RequestBody EvaluateRequest request) {
        return Result.success(performanceService.evaluate(request, SecurityUtils.getCurrentUserId()));
    }
}
