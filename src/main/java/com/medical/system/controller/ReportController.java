package com.medical.system.controller;

import com.medical.system.common.ExcelExportUtil;
import com.medical.system.common.Result;
import com.medical.system.dto.excel.ConsumptionTrendExcel;
import com.medical.system.dto.excel.CostAnalysisExcel;
import com.medical.system.dto.excel.DeptRankingExcel;
import com.medical.system.dto.response.DashboardResponse;
import com.medical.system.service.impl.ReportServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportServiceImpl reportService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('menu:dashboard')")
    public Result<DashboardResponse> getDashboard() {
        return Result.success(reportService.getDashboard());
    }

    @GetMapping("/consumption-trend")
    @PreAuthorize("hasAuthority('menu:report')")
    public Result<List<Map<String, Object>>> getConsumptionTrend(
            @RequestParam(defaultValue = "6") int months) {
        return Result.success(reportService.getConsumptionTrend(months));
    }

    @GetMapping("/department-ranking")
    @PreAuthorize("hasAuthority('menu:report')")
    public Result<List<Map<String, Object>>> getDeptRanking(
            @RequestParam(defaultValue = "30") int days) {
        return Result.success(reportService.getDeptRanking(days));
    }

    @GetMapping("/cost-analysis")
    @PreAuthorize("hasAuthority('menu:report')")
    public Result<List<Map<String, Object>>> getCostAnalysis(
            @RequestParam(defaultValue = "6") int months) {
        return Result.success(reportService.getCostAnalysis(months));
    }

    @GetMapping("/bi-dashboard")
    @PreAuthorize("hasAuthority('menu:report')")
    public Result<Map<String, Object>> getBiDashboard() {
        return Result.success(reportService.getBiDashboard());
    }

    @GetMapping("/consumption-trend/export")
    @PreAuthorize("hasAuthority('menu:report')")
    public void exportConsumptionTrend(@RequestParam(defaultValue = "6") int months,
                                        HttpServletResponse response) {
        List<Map<String, Object>> data = reportService.getConsumptionTrend(months);
        List<ConsumptionTrendExcel> excelData = new ArrayList<>();
        for (Map<String, Object> item : data) {
            ConsumptionTrendExcel row = new ConsumptionTrendExcel();
            row.setMonth(String.valueOf(item.get("month")));
            row.setQuantity(item.get("total") != null ? ((Number) item.get("total")).longValue() : 0L);
            row.setAmount(item.get("total") != null ? ((Number) item.get("total")).doubleValue() * 5 : 0.0);
            excelData.add(row);
        }
        ExcelExportUtil.export(response, "消耗趋势报表", ConsumptionTrendExcel.class, excelData);
    }

    @GetMapping("/department-ranking/export")
    @PreAuthorize("hasAuthority('menu:report')")
    public void exportDeptRanking(@RequestParam(defaultValue = "30") int days,
                                   HttpServletResponse response) {
        List<Map<String, Object>> data = reportService.getDeptRanking(days);
        List<DeptRankingExcel> excelData = new ArrayList<>();
        for (Map<String, Object> item : data) {
            DeptRankingExcel row = new DeptRankingExcel();
            row.setDeptName(item.get("deptName") != null ? String.valueOf(item.get("deptName")) : "未知科室");
            row.setQuantity(item.get("totalQuantity") != null ? ((Number) item.get("totalQuantity")).longValue() : 0L);
            row.setAmount(item.get("totalAmount") != null ? ((Number) item.get("totalAmount")).doubleValue() : 0.0);
            row.setRank(item.get("rank") != null ? ((Number) item.get("rank")).intValue() : 0);
            excelData.add(row);
        }
        ExcelExportUtil.export(response, "科室排名报表", DeptRankingExcel.class, excelData);
    }

    @GetMapping("/cost-analysis/export")
    @PreAuthorize("hasAuthority('menu:report')")
    public void exportCostAnalysis(@RequestParam(defaultValue = "6") int months,
                                    HttpServletResponse response) {
        List<Map<String, Object>> data = reportService.getCostAnalysis(months);
        List<CostAnalysisExcel> excelData = new ArrayList<>();
        for (Map<String, Object> item : data) {
            CostAnalysisExcel row = new CostAnalysisExcel();
            row.setMonth(String.valueOf(item.get("month")));
            row.setCategory("耗材总计");
            row.setCost(item.get("consumptionCost") != null ? ((Number) item.get("consumptionCost")).doubleValue() : 0.0);
            excelData.add(row);
        }
        ExcelExportUtil.export(response, "成本分析报表", CostAnalysisExcel.class, excelData);
    }
}
