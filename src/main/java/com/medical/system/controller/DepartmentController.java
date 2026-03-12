package com.medical.system.controller;

import com.medical.system.common.Result;
import com.medical.system.dto.request.CreateDepartmentRequest;
import com.medical.system.dto.response.DepartmentResponse;
import com.medical.system.service.impl.DepartmentServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentServiceImpl departmentService;

    // 查找类接口：全院通用下拉数据，所有已认证用户可访问
    @GetMapping("/tree")
    public Result<List<DepartmentResponse>> getDepartmentTree() {
        return Result.success(departmentService.getDepartmentTree());
    }

    @GetMapping
    public Result<List<DepartmentResponse>> getAllDepartments() {
        return Result.success(departmentService.getAllDepartments());
    }

    @GetMapping("/{id}")
    public Result<DepartmentResponse> getDepartmentById(@PathVariable Long id) {
        return Result.success(departmentService.getDepartmentById(id));
    }

    // 管理类接口：仅限有 menu:department 权限的角色
    @PostMapping
    @PreAuthorize("hasAuthority('menu:department')")
    public Result<DepartmentResponse> createDepartment(@Valid @RequestBody CreateDepartmentRequest request) {
        return Result.success(departmentService.createDepartment(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('menu:department')")
    public Result<DepartmentResponse> updateDepartment(@PathVariable Long id,
                                                        @RequestBody CreateDepartmentRequest request) {
        return Result.success(departmentService.updateDepartment(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('menu:department')")
    public Result<Void> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return Result.success();
    }
}
