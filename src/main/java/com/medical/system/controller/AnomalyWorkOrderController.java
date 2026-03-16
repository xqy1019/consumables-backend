package com.medical.system.controller;

import com.medical.system.common.ExcelExportUtil;
import com.medical.system.common.PageResult;
import com.medical.system.common.Result;
import com.medical.system.dto.excel.WorkOrderExcel;
import com.medical.system.service.impl.AnomalyWorkOrderService;
import com.medical.system.service.impl.AnomalyWorkOrderService.*;
import com.medical.system.security.SecurityUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/anomaly-work-orders")
@RequiredArgsConstructor
public class AnomalyWorkOrderController {

    private final AnomalyWorkOrderService anomalyWorkOrderService;

    // ── 工单查询 ──

    @GetMapping
    public Result<PageResult<WorkOrderVO>> getAllWorkOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String status) {
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page - 1, size);
        return Result.success(anomalyWorkOrderService.getWorkOrdersFiltered(deptId, priority, status, pageable));
    }

    @GetMapping("/{id}")
    public Result<WorkOrderVO> getWorkOrderById(@PathVariable Long id) {
        return Result.success(anomalyWorkOrderService.getWorkOrderById(id));
    }

    @GetMapping("/stats")
    public Result<WorkOrderStatsVO> getStats() {
        return Result.success(anomalyWorkOrderService.getStats());
    }

    // ── 工单操作 ──

    @PostMapping
    public Result<WorkOrderVO> createWorkOrder(@RequestBody CreateWorkOrderInput input) {
        return Result.success(anomalyWorkOrderService.createWorkOrder(input, SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/{id}/assign")
    public Result<WorkOrderVO> assignWorkOrder(@PathVariable Long id, @RequestParam Long assigneeId) {
        return Result.success(anomalyWorkOrderService.assignWorkOrder(id, assigneeId));
    }

    @PutMapping("/{id}/resolve")
    public Result<WorkOrderVO> resolveWorkOrder(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.success(anomalyWorkOrderService.resolveWorkOrder(id, body.get("resolution")));
    }

    @PutMapping("/{id}/close")
    public Result<WorkOrderVO> closeWorkOrder(@PathVariable Long id) {
        return Result.success(anomalyWorkOrderService.closeWorkOrder(id));
    }

    // ── 批量操作 ──

    public record BatchAssignRequest(List<Long> ids, Long assigneeId) {}
    public record BatchCloseRequest(List<Long> ids) {}

    @PutMapping("/batch-assign")
    @PreAuthorize("hasAuthority('inventory:edit')")
    public Result<Integer> batchAssign(@RequestBody BatchAssignRequest req) {
        return Result.success(anomalyWorkOrderService.batchAssign(req.ids(), req.assigneeId()));
    }

    @PutMapping("/batch-close")
    @PreAuthorize("hasAuthority('inventory:edit')")
    public Result<Integer> batchClose(@RequestBody BatchCloseRequest req) {
        return Result.success(anomalyWorkOrderService.batchClose(req.ids()));
    }

    @PostMapping("/{id}/comments")
    public Result<CommentVO> addComment(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.success(anomalyWorkOrderService.addComment(id, SecurityUtils.getCurrentUserId(), body.get("content")));
    }

    // ── 导出 ──

    @GetMapping("/export")
    public void exportWorkOrders(HttpServletResponse response) {
        List<WorkOrderVO> orders = anomalyWorkOrderService.getAllWorkOrders();
        List<WorkOrderExcel> data = new java.util.ArrayList<>();
        for (WorkOrderVO o : orders) {
            WorkOrderExcel row = new WorkOrderExcel();
            row.setId(o.getId());
            row.setDeptName(o.getDeptName());
            row.setMaterialName(o.getMaterialName());
            row.setAnomalyType("DANGER".equals(o.getAnomalyType()) ? "严重" : "警告");
            row.setDeviationRate(o.getDeviationRate() != null ? String.format("%.0f%%", o.getDeviationRate() * 100) : "-");
            row.setPriority("HIGH".equals(o.getPriority()) ? "高" : "NORMAL".equals(o.getPriority()) ? "中" : "低");
            row.setStatus(switch (o.getStatus()) {
                case "OPEN" -> "待处理";
                case "IN_PROGRESS" -> "处理中";
                case "RESOLVED" -> "已解决";
                case "CLOSED" -> "已关闭";
                default -> o.getStatus();
            });
            row.setAssignedToName(o.getAssignedToName() != null ? o.getAssignedToName() : "未分配");
            row.setCreatedByName(o.getCreatedByName());
            row.setCreatedAt(o.getCreatedAt() != null ? o.getCreatedAt().toString() : "");
            row.setResolution(o.getResolution());
            data.add(row);
        }
        ExcelExportUtil.export(response, "异常工单列表", WorkOrderExcel.class, data);
    }
}
