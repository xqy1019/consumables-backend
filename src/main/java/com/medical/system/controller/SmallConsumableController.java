package com.medical.system.controller;

import com.medical.system.annotation.Log;
import com.medical.system.common.ExcelExportUtil;
import com.medical.system.common.Result;
import com.medical.system.dto.ConsumptionSummaryVO;
import com.medical.system.dto.excel.AnomalyExcel;
import com.medical.system.dto.excel.MonthlyReportAnomalyExcel;
import com.medical.system.dto.excel.MonthlyReportParSuggestionExcel;
import com.medical.system.dto.excel.ParLevelExcel;
import com.medical.system.security.SecurityUtils;
import com.medical.system.service.impl.SmallConsumableService;
import com.medical.system.service.impl.SmallConsumableService.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/small-consumables")
@RequiredArgsConstructor
public class SmallConsumableController {

    private final SmallConsumableService service;

    // ============ 科室定数管理 ============

    @GetMapping("/par-levels")
    @PreAuthorize("hasAuthority('menu:inventory')")
    public Result<List<ParLevelVO>> getParLevels(
            @RequestParam(required = false) Long deptId) {
        return Result.success(service.getParLevels(deptId));
    }

    @Log(module = "小耗材管理", action = "保存定数配置")
    @PostMapping("/par-levels")
    @PreAuthorize("hasAuthority('inventory:edit')")
    public Result<ParLevelVO> saveParLevel(@RequestBody SaveParLevelRequest req) {
        return Result.success(service.saveParLevel(req));
    }

    @Log(module = "小耗材管理", action = "删除定数配置")
    @DeleteMapping("/par-levels/{id}")
    @PreAuthorize("hasAuthority('inventory:edit')")
    public Result<Void> deleteParLevel(@PathVariable Long id) {
        service.deleteParLevel(id);
        return Result.success(null);
    }

    // ============ 诊疗消耗包管理 ============

    @GetMapping("/templates")
    @PreAuthorize("hasAuthority('menu:inventory')")
    public Result<List<TemplateVO>> getTemplates() {
        return Result.success(service.getTemplates());
    }

    @Log(module = "小耗材管理", action = "新建消耗包模板")
    @PostMapping("/templates")
    @PreAuthorize("hasAuthority('inventory:edit')")
    public Result<TemplateVO> createTemplate(@RequestBody SaveTemplateRequest req) {
        return Result.success(service.saveTemplate(req, SecurityUtils.getCurrentUserId()));
    }

    @Log(module = "小耗材管理", action = "更新消耗包模板")
    @PutMapping("/templates/{id}")
    @PreAuthorize("hasAuthority('inventory:edit')")
    public Result<TemplateVO> updateTemplate(@PathVariable Long id,
                                              @RequestBody SaveTemplateRequest req) {
        return Result.success(service.updateTemplate(id, req));
    }

    @Log(module = "小耗材管理", action = "删除消耗包模板")
    @DeleteMapping("/templates/{id}")
    @PreAuthorize("hasAuthority('inventory:edit')")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        service.deleteTemplate(id);
        return Result.success(null);
    }

    // ============ 诊疗操作记录 ============

    @GetMapping("/records")
    @PreAuthorize("hasAuthority('menu:inventory')")
    public Result<List<RecordVO>> getRecords(
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String yearMonth) {
        return Result.success(service.getRecords(deptId, yearMonth));
    }

    @Log(module = "小耗材管理", action = "记录诊疗操作")
    @PostMapping("/records")
    @PreAuthorize("hasAuthority('inventory:edit')")
    public Result<RecordVO> addRecord(@RequestBody RecordRequest req) {
        return Result.success(service.addRecord(req, SecurityUtils.getCurrentUserId()));
    }

    // ============ 消耗汇总（自动生成） ============

    @GetMapping("/consumption-summary")
    @PreAuthorize("hasAuthority('menu:inventory')")
    public Result<List<ConsumptionSummaryVO>> getConsumptionSummary(
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String yearMonth) {
        return Result.success(service.getConsumptionSummary(deptId, yearMonth));
    }

    // ============ 科室消耗预测 ============

    @GetMapping("/consumption-forecast")
    @PreAuthorize("hasAuthority('menu:inventory')")
    public Result<List<ConsumptionForecastVO>> getConsumptionForecast(
            @RequestParam Long deptId) {
        return Result.success(service.getConsumptionForecast(deptId));
    }

    // ============ 消耗异常分析 ============

    @GetMapping("/anomaly")
    @PreAuthorize("hasAuthority('menu:inventory')")
    public Result<AnomalySummaryVO> getAnomalySummary(
            @RequestParam(required = false) String yearMonth) {
        return Result.success(service.getAnomalySummary(yearMonth));
    }

    // ============ 异常趋势 ============

    @GetMapping("/anomaly/trend")
    @PreAuthorize("hasAuthority('menu:inventory')")
    public Result<List<AnomalyTrendVO>> getAnomalyTrend(
            @RequestParam(defaultValue = "6") int months) {
        return Result.success(service.getAnomalyTrend(months));
    }

    // ============ AI 增强异常分析 ============

    @GetMapping("/anomaly/ai-analysis")
    @PreAuthorize("hasAuthority('menu:ai')")
    public Result<List<AnomalyAnalysisVO>> getAiAnomalyAnalysis(
            @RequestParam(required = false) String yearMonth) {
        return Result.success(service.getAiAnomalyAnalysis(yearMonth));
    }

    // ============ 异常自动生成工单 ============

    @Log(module = "小耗材管理", action = "异常自动生成工单")
    @PostMapping("/anomaly/auto-create-orders")
    @PreAuthorize("hasAuthority('inventory:edit')")
    public Result<Map<String, Object>> autoCreateWorkOrders(
            @RequestParam(required = false) String yearMonth) {
        int count = service.autoCreateWorkOrdersForAnomalies(yearMonth, SecurityUtils.getCurrentUserId());
        return Result.success(Map.of("createdCount", count));
    }

    // ============ 定数智能建议 ============

    @GetMapping("/par-suggestions")
    @PreAuthorize("hasAuthority('menu:inventory')")
    public Result<List<ParSuggestionVO>> getParSuggestions() {
        return Result.success(service.getParSuggestions());
    }

    @Log(module = "小耗材管理", action = "应用定数建议")
    @PostMapping("/par-suggestions/apply")
    @PreAuthorize("hasAuthority('inventory:edit')")
    public Result<Void> applyParSuggestion(@RequestBody Map<String, Object> body) {
        Long deptId = Long.parseLong(body.get("deptId").toString());
        Long materialId = Long.parseLong(body.get("materialId").toString());
        int newPar = Integer.parseInt(body.get("suggestedPar").toString());
        int newMin = Integer.parseInt(body.get("suggestedMin").toString());
        service.applyParSuggestion(deptId, materialId, newPar, newMin);
        return Result.success(null);
    }

    // ============ Excel 导出 ============

    @GetMapping("/par-levels/export")
    @PreAuthorize("hasAuthority('menu:inventory')")
    public void exportParLevels(@RequestParam(required = false) Long deptId,
                                 HttpServletResponse response) {
        List<ParLevelVO> parLevels = service.getParLevels(deptId);
        List<ParLevelExcel> data = new ArrayList<>();
        for (ParLevelVO p : parLevels) {
            ParLevelExcel row = new ParLevelExcel();
            row.setDeptName(p.getDeptName());
            row.setMaterialName(p.getMaterialName());
            row.setSpecification(p.getMaterialSpec());
            row.setUnit(p.getUnit());
            row.setParQuantity(p.getParQuantity() != null ? p.getParQuantity().intValue() : 0);
            row.setMinQuantity(p.getMinQuantity() != null ? p.getMinQuantity().intValue() : 0);
            data.add(row);
        }
        ExcelExportUtil.export(response, "科室定数配置", ParLevelExcel.class, data);
    }

    @GetMapping("/anomaly/export")
    @PreAuthorize("hasAuthority('menu:inventory')")
    public void exportAnomaly(@RequestParam(required = false) String yearMonth,
                               HttpServletResponse response) {
        AnomalySummaryVO summary = service.getAnomalySummary(yearMonth);
        List<AnomalyExcel> data = new ArrayList<>();
        if (summary.getAnomalies() != null) {
            for (AnomalyVO a : summary.getAnomalies()) {
                AnomalyExcel row = new AnomalyExcel();
                row.setDeptName(a.getDeptName());
                row.setMaterialName(a.getMaterialName());
                row.setLevel(a.getDeviationRate() > 0.5 ? "DANGER" : a.getDeviationRate() > 0.3 ? "WARNING" : "NORMAL");
                row.setDeviationRate(String.format("%.0f%%", a.getDeviationRate() * 100));
                row.setCurrentUsage(a.getThisMonthQty() != null ? a.getThisMonthQty().intValue() : 0);
                row.setHistoricalAvg(a.getBaselineQty() != null ? a.getBaselineQty().doubleValue() : 0);
                data.add(row);
            }
        }
        ExcelExportUtil.export(response, "消耗异常报告", AnomalyExcel.class, data);
    }

    // ============ 月度报告导出 ============

    @GetMapping("/monthly-report/export")
    @PreAuthorize("hasAuthority('menu:inventory')")
    public void exportMonthlyReport(@RequestParam(required = false) String yearMonth,
                                     HttpServletResponse response) {
        // Sheet1: 异常明细
        AnomalySummaryVO summary = service.getAnomalySummary(yearMonth);
        List<MonthlyReportAnomalyExcel> anomalyData = new ArrayList<>();
        if (summary.getAnomalies() != null) {
            for (AnomalyVO a : summary.getAnomalies()) {
                MonthlyReportAnomalyExcel row = new MonthlyReportAnomalyExcel();
                row.setDeptName(a.getDeptName());
                row.setMaterialName(a.getMaterialName());
                row.setUnit(a.getUnit());
                row.setLevel("DANGER".equals(a.getLevel()) ? "严重" : "WARNING".equals(a.getLevel()) ? "预警" : "正常");
                row.setDeviationRate(a.getDeviationRate() != null ? String.format("%.0f%%", a.getDeviationRate() * 100) : "-");
                row.setCurrentUsage(a.getThisMonthQty() != null ? a.getThisMonthQty().intValue() : 0);
                row.setHistoricalAvg(a.getBaselineQty() != null ? a.getBaselineQty().doubleValue() : 0);
                row.setMonthlyLimit(a.getMonthlyLimit() != null ? a.getMonthlyLimit().toString() : "-");
                row.setOverLimit(Boolean.TRUE.equals(a.getOverLimit()) ? "是" : "否");
                anomalyData.add(row);
            }
        }

        // Sheet2: 定数建议
        List<ParSuggestionVO> suggestions = service.getParSuggestions();
        List<MonthlyReportParSuggestionExcel> suggestionData = new ArrayList<>();
        for (ParSuggestionVO s : suggestions) {
            MonthlyReportParSuggestionExcel row = new MonthlyReportParSuggestionExcel();
            row.setDeptName(s.getDeptName());
            row.setMaterialName(s.getMaterialName());
            row.setUnit(s.getUnit());
            row.setDirection("UP".equals(s.getDirection()) ? "上调" : "DOWN".equals(s.getDirection()) ? "下调" : "保持");
            row.setCurrentPar(s.getCurrentPar());
            row.setSuggestedPar(s.getSuggestedPar());
            row.setCurrentMin(s.getCurrentMin());
            row.setSuggestedMin(s.getSuggestedMin());
            row.setAvgMonthlyUsage(s.getAvgMonthlyUsage());
            row.setReason(s.getReason());
            suggestionData.add(row);
        }

        String ym = yearMonth != null ? yearMonth : java.time.YearMonth.now().toString();
        ExcelExportUtil.exportMultiSheet(response, ym + " 月度精细化管理报告", List.of(
                new ExcelExportUtil.SheetConfig<>("异常明细", MonthlyReportAnomalyExcel.class, anomalyData),
                new ExcelExportUtil.SheetConfig<>("定数建议", MonthlyReportParSuggestionExcel.class, suggestionData)
        ));
    }
}
