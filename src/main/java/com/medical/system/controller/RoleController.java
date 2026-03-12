package com.medical.system.controller;

import com.medical.system.common.Result;
import com.medical.system.dto.request.CreateRoleRequest;
import com.medical.system.dto.response.PermissionResponse;
import com.medical.system.dto.response.RoleResponse;
import com.medical.system.service.impl.RoleServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleServiceImpl roleService;

    @GetMapping
    public Result<List<RoleResponse>> getAllRoles() {
        return Result.success(roleService.getAllRoles());
    }

    @GetMapping("/permissions")
    public Result<List<PermissionResponse>> getAllPermissions() {
        return Result.success(roleService.getAllPermissions());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('role:edit')")
    public Result<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
        return Result.success(roleService.createRole(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('role:edit')")
    public Result<RoleResponse> updateRole(@PathVariable Long id, @RequestBody CreateRoleRequest request) {
        return Result.success(roleService.updateRole(id, request));
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('role:edit')")
    public Result<RoleResponse> assignPermissions(@PathVariable Long id,
                                                   @RequestBody Map<String, List<Long>> body) {
        return Result.success(roleService.assignPermissions(id, body.get("permissionIds")));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('role:edit')")
    public Result<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.success();
    }
}
