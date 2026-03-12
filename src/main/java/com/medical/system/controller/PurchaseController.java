package com.medical.system.controller;

import com.medical.system.annotation.Log;
import com.medical.system.common.PageResult;
import com.medical.system.common.Result;
import com.medical.system.dto.request.ApprovalRequest;
import com.medical.system.dto.request.CreateContractRequest;
import com.medical.system.dto.request.CreateInquiryRequest;
import com.medical.system.dto.request.CreatePurchaseRequisitionRequest;
import com.medical.system.entity.*;
import com.medical.system.service.impl.PurchaseServiceImpl;
import com.medical.system.service.impl.PurchaseServiceImpl.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/purchase")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseServiceImpl purchaseService;

    // ========== 请购单 ==========
    @GetMapping("/requisitions")
    @PreAuthorize("hasAuthority('menu:purchase')")
    public Result<PageResult<RequisitionVO>> getRequisitions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(purchaseService.getRequisitions(keyword, status,
                PageRequest.of(page - 1, size, Sort.by("id").descending())));
    }

    @Log(module = "采购管理", action = "创建请购单")
    @PostMapping("/requisitions")
    @PreAuthorize("hasAuthority('purchase:edit')")
    public Result<RequisitionVO> createRequisition(@Valid @RequestBody CreatePurchaseRequisitionRequest request) {
        PurchaseRequisition req = new PurchaseRequisition();
        req.setDeptId(request.getDeptId());
        req.setRequiredDate(request.getRequiredDate());
        req.setRemark(request.getRemark());
        List<PurchaseRequisitionItem> items = request.getItems().stream().map(i -> {
            PurchaseRequisitionItem item = new PurchaseRequisitionItem();
            item.setMaterialId(i.getMaterialId());
            item.setQuantity(i.getQuantity());
            item.setEstimatedPrice(i.getEstimatedPrice());
            item.setRemark(i.getRemark());
            return item;
        }).collect(java.util.stream.Collectors.toList());
        return Result.success(purchaseService.createRequisition(req, items));
    }

    @GetMapping("/requisitions/{id}")
    @PreAuthorize("hasAuthority('menu:purchase')")
    public Result<RequisitionVO> getRequisitionDetail(@PathVariable Long id) {
        return Result.success(purchaseService.getRequisitionDetail(id));
    }

    @PutMapping("/requisitions/{id}/submit")
    @PreAuthorize("hasAuthority('purchase:edit')")
    public Result<Void> submitRequisition(@PathVariable Long id) {
        purchaseService.submitRequisition(id);
        return Result.success();
    }

    @Log(module = "采购管理", action = "审批请购单")
    @PutMapping("/requisitions/{id}/approve")
    @PreAuthorize("hasAuthority('purchase:edit')")
    public Result<Void> approveRequisition(@PathVariable Long id, @RequestBody ApprovalRequest body) {
        purchaseService.approveRequisition(id, body.getRemark() != null ? body.getRemark() : "审核通过");
        return Result.success();
    }

    @Log(module = "采购管理", action = "驳回请购单")
    @PutMapping("/requisitions/{id}/reject")
    @PreAuthorize("hasAuthority('purchase:edit')")
    public Result<Void> rejectRequisition(@PathVariable Long id, @RequestBody ApprovalRequest body) {
        purchaseService.rejectRequisition(id, body.getRemark() != null ? body.getRemark() : "驳回");
        return Result.success();
    }

    @Log(module = "采购管理", action = "采购收货")
    @PostMapping("/requisitions/{id}/receive")
    @PreAuthorize("hasAuthority('purchase:edit')")
    public Result<Void> receiveGoods(@PathVariable Long id, @RequestBody List<PurchaseServiceImpl.ReceiveItemVO> items) {
        purchaseService.receiveGoods(id, items);
        return Result.success();
    }

    // ========== 询价单 ==========
    @GetMapping("/inquiries")
    @PreAuthorize("hasAuthority('menu:purchase')")
    public Result<PageResult<InquiryVO>> getInquiries(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(purchaseService.getInquiries(keyword, status,
                PageRequest.of(page - 1, size, Sort.by("id").descending())));
    }

    @PostMapping("/inquiries")
    @PreAuthorize("hasAuthority('purchase:edit')")
    public Result<InquiryVO> createInquiry(@Valid @RequestBody CreateInquiryRequest request) {
        PurchaseInquiry inquiry = new PurchaseInquiry();
        inquiry.setSupplierId(request.getSupplierId());
        inquiry.setReqId(request.getReqId());
        inquiry.setValidDate(request.getValidDate());
        inquiry.setRemark(request.getRemark());
        List<CreateInquiryRequest.ItemRequest> rawItems = request.getItems() != null
                ? request.getItems() : List.of();
        List<PurchaseInquiryItem> items = rawItems.stream().map(i -> {
            PurchaseInquiryItem item = new PurchaseInquiryItem();
            item.setMaterialId(i.getMaterialId());
            item.setQuantity(i.getQuantity());
            item.setQuotedPrice(i.getQuotedPrice());
            item.setDeliveryDays(i.getDeliveryDays());
            return item;
        }).collect(java.util.stream.Collectors.toList());
        return Result.success(purchaseService.createInquiry(inquiry, items));
    }

    @GetMapping("/inquiries/{id}")
    @PreAuthorize("hasAuthority('menu:purchase')")
    public Result<InquiryVO> getInquiryDetail(@PathVariable Long id) {
        return Result.success(purchaseService.getInquiryDetail(id));
    }

    @PutMapping("/inquiries/{id}/confirm")
    @PreAuthorize("hasAuthority('purchase:edit')")
    public Result<Void> confirmInquiry(@PathVariable Long id) {
        purchaseService.confirmInquiry(id);
        return Result.success();
    }

    // ========== 合同 ==========
    @GetMapping("/contracts")
    @PreAuthorize("hasAuthority('menu:purchase')")
    public Result<PageResult<ContractVO>> getContracts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(purchaseService.getContracts(keyword, status,
                PageRequest.of(page - 1, size, Sort.by("id").descending())));
    }

    @PostMapping("/contracts")
    @PreAuthorize("hasAuthority('purchase:edit')")
    public Result<ContractVO> createContract(@Valid @RequestBody CreateContractRequest request) {
        PurchaseContract contract = new PurchaseContract();
        contract.setSupplierId(request.getSupplierId());
        contract.setContractDate(request.getContractDate());
        contract.setDeliveryDate(request.getDeliveryDate());
        contract.setInquiryId(request.getInquiryId());
        contract.setRemark(request.getRemark());
        contract.setTotalAmount(java.math.BigDecimal.ZERO);
        List<PurchaseContractItem> items = request.getItems().stream().map(i -> {
            PurchaseContractItem item = new PurchaseContractItem();
            item.setMaterialId(i.getMaterialId());
            item.setQuantity(i.getQuantity());
            item.setUnitPrice(i.getUnitPrice());
            item.setTotalPrice(i.getUnitPrice().multiply(java.math.BigDecimal.valueOf(i.getQuantity())));
            return item;
        }).collect(java.util.stream.Collectors.toList());
        return Result.success(purchaseService.createContract(contract, items));
    }

    @GetMapping("/contracts/{id}")
    @PreAuthorize("hasAuthority('menu:purchase')")
    public Result<ContractVO> getContractDetail(@PathVariable Long id) {
        return Result.success(purchaseService.getContractDetail(id));
    }

    @PutMapping("/contracts/{id}/execute")
    @PreAuthorize("hasAuthority('purchase:edit')")
    public Result<Void> executeContract(@PathVariable Long id) {
        purchaseService.executeContract(id);
        return Result.success();
    }

    @GetMapping("/auto-suggestions")
    @PreAuthorize("hasAuthority('menu:purchase')")
    public Result<List<AutoSuggestionVO>> getAutoSuggestions() {
        return Result.success(purchaseService.getAutoSuggestions());
    }
}
