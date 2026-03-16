package com.medical.system.controller;

import com.medical.system.annotation.Log;
import com.medical.system.common.PageResult;
import com.medical.system.common.Result;
import com.medical.system.dto.request.InboundRequest;
import com.medical.system.dto.request.InspectRequest;
import com.medical.system.dto.request.OutboundRequest;
import com.medical.system.dto.response.InventoryResponse;
import com.medical.system.security.SecurityUtils;
import com.medical.system.service.impl.InventoryServiceImpl;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Validated
public class InventoryController {

    private final InventoryServiceImpl inventoryService;

    @GetMapping
    @PreAuthorize("hasAuthority('menu:inventory')")
    public Result<PageResult<InventoryResponse>> getInventory(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        PageRequest pageable = PageRequest.of(page - 1, size);
        return Result.success(inventoryService.getInventory(keyword, status, pageable));
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAuthority('menu:inventory')")
    public Result<List<InventoryResponse>> getAlerts() {
        return Result.success(inventoryService.getAlerts());
    }

    @Log(module = "库存管理", action = "入库")
    @PostMapping("/inbound")
    @PreAuthorize("hasAuthority('inventory:edit')")
    public Result<InventoryResponse> inbound(@Valid @RequestBody InboundRequest request) {
        return Result.success(inventoryService.inbound(request));
    }

    @Log(module = "库存管理", action = "出库")
    @PostMapping("/outbound")
    @PreAuthorize("hasAuthority('inventory:edit')")
    public Result<Void> outbound(@Valid @RequestBody OutboundRequest request) {
        inventoryService.outbound(request);
        return Result.success();
    }

    @GetMapping("/inspections")
    @PreAuthorize("hasAuthority('menu:inventory')")
    public Result<PageResult<InventoryResponse>> getInspections(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String inspectionStatus,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        PageRequest pageable = PageRequest.of(page - 1, size);
        return Result.success(inventoryService.getInspections(keyword, inspectionStatus, pageable));
    }

    @Log(module = "库存管理", action = "验收")
    @PutMapping("/{id}/inspect")
    @PreAuthorize("hasAuthority('inventory:edit')")
    public Result<InventoryResponse> inspect(@PathVariable Long id,
                                             @Valid @RequestBody InspectRequest request) {
        return Result.success(inventoryService.inspect(id, request, SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/batch-suggestion")
    @PreAuthorize("hasAuthority('menu:inventory')")
    public Result<List<InventoryResponse>> getBatchSuggestion(@RequestParam Long materialId) {
        return Result.success(inventoryService.getBatchSuggestion(materialId));
    }
}
