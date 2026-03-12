package com.medical.system.service.impl;

import com.medical.system.dto.request.CreateRoleRequest;
import com.medical.system.dto.response.PermissionResponse;
import com.medical.system.dto.response.RoleResponse;
import com.medical.system.entity.Permission;
import com.medical.system.entity.Role;
import com.medical.system.exception.BusinessException;
import com.medical.system.repository.PermissionRepository;
import com.medical.system.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::convertToResponse).collect(Collectors.toList());
    }

    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        if (roleRepository.existsByRoleCode(request.getRoleCode())) {
            throw new BusinessException("角色编码已存在");
        }
        Role role = new Role();
        role.setRoleName(request.getRoleName());
        role.setRoleCode(request.getRoleCode());
        role.setDescription(request.getDescription());
        return convertToResponse(roleRepository.save(role));
    }

    @Transactional
    public RoleResponse updateRole(Long id, CreateRoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("角色不存在"));
        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());
        return convertToResponse(roleRepository.save(role));
    }

    @Transactional
    public void deleteRole(Long id) {
        if (!roleRepository.existsById(id)) throw new BusinessException("角色不存在");
        roleRepository.deleteById(id);
    }

    @Transactional
    public RoleResponse assignPermissions(Long roleId, List<Long> permissionIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException("角色不存在"));
        Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(permissionIds));
        role.setPermissions(permissions);
        return convertToResponse(roleRepository.save(role));
    }

    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAllByOrderBySortOrderAsc().stream()
                .map(this::convertPermToResponse).collect(Collectors.toList());
    }

    public RoleResponse convertToResponse(Role role) {
        RoleResponse response = new RoleResponse();
        response.setId(role.getId());
        response.setRoleName(role.getRoleName());
        response.setRoleCode(role.getRoleCode());
        response.setDescription(role.getDescription());
        response.setStatus(role.getStatus());
        response.setCreateTime(role.getCreateTime());
        Set<PermissionResponse> permResponses = role.getPermissions().stream()
                .map(this::convertPermToResponse).collect(Collectors.toSet());
        response.setPermissions(permResponses);
        return response;
    }

    private PermissionResponse convertPermToResponse(Permission perm) {
        PermissionResponse r = new PermissionResponse();
        r.setId(perm.getId());
        r.setPermissionCode(perm.getPermissionCode());
        r.setPermissionName(perm.getPermissionName());
        r.setType(perm.getType());
        r.setDescription(perm.getDescription());
        r.setSortOrder(perm.getSortOrder());
        return r;
    }
}
