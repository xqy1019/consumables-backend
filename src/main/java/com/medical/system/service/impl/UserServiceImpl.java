package com.medical.system.service.impl;

import com.medical.system.common.PageResult;
import com.medical.system.dto.request.CreateUserRequest;
import com.medical.system.dto.request.UpdateUserRequest;
import com.medical.system.dto.response.RoleResponse;
import com.medical.system.dto.response.UserResponse;
import com.medical.system.entity.Role;
import com.medical.system.entity.User;
import com.medical.system.exception.BusinessException;
import com.medical.system.repository.DepartmentRepository;
import com.medical.system.repository.RoleRepository;
import com.medical.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public PageResult<UserResponse> getUsers(String keyword, Long deptId, Integer status, Pageable pageable) {
        Page<User> page = userRepository.findByConditions(keyword, deptId, status, pageable);
        List<UserResponse> records = page.getContent().stream()
                .map(this::convertToResponse).collect(Collectors.toList());
        return PageResult.of(records, page.getTotalElements(), pageable.getPageNumber() + 1, pageable.getPageSize());
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        return convertToResponse(user);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setDeptId(request.getDeptId());
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            Set<Role> roles = new HashSet<>(roleRepository.findAllById(request.getRoleIds()));
            user.setRoles(roles);
        }
        return convertToResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        if (request.getRealName() != null) user.setRealName(request.getRealName());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getDeptId() != null) user.setDeptId(request.getDeptId());
        if (request.getRoleIds() != null) {
            Set<Role> roles = new HashSet<>(roleRepository.findAllById(request.getRoleIds()));
            user.setRoles(roles);
        }
        return convertToResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) throw new BusinessException("用户不存在");
        userRepository.deleteById(id);
    }

    @Transactional
    public void updateStatus(Long id, Integer status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        user.setStatus(status);
        userRepository.save(user);
    }

    public UserResponse convertToResponse(User user) {
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
            rr.setId(role.getId()); rr.setRoleName(role.getRoleName());
            rr.setRoleCode(role.getRoleCode()); rr.setDescription(role.getDescription());
            rr.setStatus(role.getStatus());
            return rr;
        }).collect(Collectors.toSet());
        response.setRoles(roleResponses);
        return response;
    }
}
