package com.medical.system.service.impl;

import com.medical.system.dto.request.LoginRequest;
import com.medical.system.dto.response.LoginResponse;
import com.medical.system.dto.response.RoleResponse;
import com.medical.system.dto.response.UserResponse;
import com.medical.system.entity.User;
import com.medical.system.exception.BusinessException;
import com.medical.system.repository.DepartmentRepository;
import com.medical.system.repository.UserRepository;
import com.medical.system.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("用户不存在"));
        String token = jwtTokenProvider.generateToken(request.getUsername());
        Set<String> roles = user.getRoles().stream()
                .map(role -> role.getRoleCode())
                .collect(Collectors.toSet());
        Set<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(perm -> perm.getPermissionCode())
                .collect(Collectors.toSet());
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getRealName(), roles, permissions);
    }

    public UserResponse getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        return convertToUserResponse(user);
    }

    public UserResponse convertToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setDeptId(user.getDeptId());
        response.setStatus(user.getStatus());
        response.setCreateTime(user.getCreateTime());
        if (user.getDeptId() != null) {
            departmentRepository.findById(user.getDeptId())
                    .ifPresent(dept -> response.setDeptName(dept.getDeptName()));
        }
        Set<RoleResponse> roleResponses = user.getRoles().stream().map(role -> {
            RoleResponse rr = new RoleResponse();
            rr.setId(role.getId());
            rr.setRoleName(role.getRoleName());
            rr.setRoleCode(role.getRoleCode());
            rr.setDescription(role.getDescription());
            rr.setStatus(role.getStatus());
            return rr;
        }).collect(Collectors.toSet());
        response.setRoles(roleResponses);
        return response;
    }
}
