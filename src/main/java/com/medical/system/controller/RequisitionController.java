package com.medical.system.controller;

import com.medical.system.annotation.Log;
import com.medical.system.common.PageResult;
import com.medical.system.common.Result;
import com.medical.system.dto.request.ApprovalRequest;
import com.medical.system.dto.request.CreateRequisitionRequest;
import com.medical.system.dto.response.RequisitionResponse;
import com.medical.system.security.SecurityUtils;
import com.medical.system.service.impl.RequisitionServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/requisitions")
@RequiredArgsConstructor
public class RequisitionController {

    private final RequisitionServiceImpl requisitionService;

    @GetMapping
    @PreAuthorize("hasAuthority('menu:requisition')")
    public Result<PageResult<RequisitionResponse>> getRequisitions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Long createdBy,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by("createTime").descending());
        return Result.success(requisitionService.getRequisitions(status, deptId, createdBy, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('menu:requisition')")
    public Result<RequisitionResponse> getRequisitionById(@PathVariable Long id) {
        return Result.success(requisitionService.getRequisitionById(id));
    }

    @Log(module = "申领管理", action = "创建申领单")
    @PostMapping
    @PreAuthorize("hasAuthority('menu:requisition')")
    public Result<RequisitionResponse> createRequisition(@Valid @RequestBody CreateRequisitionRequest request) {
        return Result.success(requisitionService.createRequisition(request, SecurityUtils.getCurrentUserId()));
    }

    @Log(module = "申领管理", action = "提交申领单")
    @PutMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('menu:requisition')")
    public Result<RequisitionResponse> submitRequisition(@PathVariable Long id) {
        return Result.success(requisitionService.submitRequisition(id));
    }

    @Log(module = "申领管理", action = "审批通过申领单")
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('requisition:approve')")
    public Result<RequisitionResponse> approveRequisition(@PathVariable Long id,
                                                           @RequestBody ApprovalRequest request) {
        return Result.success(requisitionService.approveRequisition(id, SecurityUtils.getCurrentUserId(), request));
    }

    @Log(module = "申领管理", action = "驳回申领单")
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('requisition:approve')")
    public Result<RequisitionResponse> rejectRequisition(@PathVariable Long id,
                                                          @RequestBody ApprovalRequest request) {
        return Result.success(requisitionService.rejectRequisition(id, SecurityUtils.getCurrentUserId(), request));
    }

    @Log(module = "申领管理", action = "发放申领单")
    @PutMapping("/{id}/dispatch")
    @PreAuthorize("hasAuthority('requisition:dispatch')")
    public Result<RequisitionResponse> dispatchRequisition(@PathVariable Long id) {
        return Result.success(requisitionService.dispatchRequisition(id));
    }

    @PutMapping("/{id}/sign")
    @PreAuthorize("hasAuthority('menu:requisition')")
    public Result<RequisitionResponse> signRequisition(@PathVariable Long id,
                                                        @RequestBody ApprovalRequest request) {
        return Result.success(requisitionService.signRequisition(id, SecurityUtils.getCurrentUserId(), request.getRemark()));
    }
}
