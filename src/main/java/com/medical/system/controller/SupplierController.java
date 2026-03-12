package com.medical.system.controller;

import com.medical.system.common.PageResult;
import com.medical.system.common.Result;
import com.medical.system.dto.request.CreateSupplierRequest;
import com.medical.system.dto.response.SupplierResponse;
import com.medical.system.service.impl.SupplierServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierServiceImpl supplierService;

    // 分页列表：供应商管理页专用
    @GetMapping
    @PreAuthorize("hasAuthority('menu:supplier')")
    public Result<PageResult<SupplierResponse>> getSuppliers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by("createTime").descending());
        return Result.success(supplierService.getSuppliers(keyword, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('menu:supplier')")
    public Result<SupplierResponse> getSupplierById(@PathVariable Long id) {
        return Result.success(supplierService.getSupplierById(id));
    }

    // 查找类接口：用于入库/采购等表单的供应商下拉，所有已认证用户可访问
    @GetMapping("/active")
    public Result<List<SupplierResponse>> getActiveSuppliers() {
        return Result.success(supplierService.getActiveSuppliers());
    }

    // 管理类接口：仅限有 menu:supplier 权限的角色
    @PostMapping
    @PreAuthorize("hasAuthority('menu:supplier')")
    public Result<SupplierResponse> createSupplier(@Valid @RequestBody CreateSupplierRequest request) {
        return Result.success(supplierService.createSupplier(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('menu:supplier')")
    public Result<SupplierResponse> updateSupplier(@PathVariable Long id,
                                                    @RequestBody CreateSupplierRequest request) {
        return Result.success(supplierService.updateSupplier(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('menu:supplier')")
    public Result<Void> deleteSupplier(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return Result.success();
    }
}
