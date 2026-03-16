package com.medical.system.controller;

import com.medical.system.common.Result;
import com.medical.system.service.impl.DeptInventoryService;
import com.medical.system.service.impl.DeptInventoryService.*;
import com.medical.system.security.SecurityUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dept-inventory")
@RequiredArgsConstructor
public class DeptInventoryController {

    private final DeptInventoryService deptInventoryService;

    // ── 二级库存查询 ──

    @GetMapping
    public Result<List<DeptInventoryVO>> getDeptInventory(@RequestParam Long deptId) {
        return Result.success(deptInventoryService.getDeptInventory(deptId));
    }

    @GetMapping("/summary")
    public Result<List<DeptInventorySummaryVO>> getAllSummary() {
        return Result.success(deptInventoryService.getAllDeptSummary());
    }

    // ── 科室盘点 ──

    @PostMapping("/stocktaking")
    public Result<DeptStocktakingVO> createStocktaking(@RequestParam Long deptId) {
        return Result.success(deptInventoryService.createStocktaking(deptId, SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/stocktaking/{id}/complete")
    public Result<DeptStocktakingVO> completeStocktaking(
            @PathVariable Long id,
            @RequestBody List<StocktakingInput> inputs) {
        return Result.success(deptInventoryService.completeStocktaking(id, inputs));
    }

    @GetMapping("/stocktaking/history")
    public Result<List<DeptStocktakingVO>> getStocktakingHistory(@RequestParam Long deptId) {
        return Result.success(deptInventoryService.getStocktakingHistory(deptId));
    }

    // ── 科室消耗排名 ──

    @GetMapping("/consumption-ranking")
    public Result<List<DeptConsumptionRankVO>> getConsumptionRanking() {
        return Result.success(deptInventoryService.getDeptConsumptionRanking());
    }

    // ── 自动补货 ──

    @GetMapping("/replenishment/check")
    public Result<List<ReplenishmentSuggestionVO>> checkReplenishment() {
        return Result.success(deptInventoryService.checkReplenishment());
    }

    @PostMapping("/replenishment/execute")
    public Result<ReplenishmentResult> executeReplenishment() {
        int count = deptInventoryService.executeAutoReplenishment(SecurityUtils.getCurrentUserId());
        ReplenishmentResult r = new ReplenishmentResult();
        r.setRequisitionCount(count);
        r.setMessage(count > 0 ? "已为" + count + "个科室生成补货申领单（草稿）" : "所有科室库存充足，无需补货");
        return Result.success(r);
    }

    @Data
    public static class ReplenishmentResult {
        private int requisitionCount;
        private String message;
    }
}
