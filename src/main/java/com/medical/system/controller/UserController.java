package com.medical.system.controller;

import com.medical.system.annotation.Log;
import com.medical.system.common.PageResult;
import com.medical.system.common.Result;
import com.medical.system.dto.request.CreateUserRequest;
import com.medical.system.dto.request.UpdateUserRequest;
import com.medical.system.dto.response.UserResponse;
import com.medical.system.service.impl.UserServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserServiceImpl userService;

    @GetMapping
    public Result<PageResult<UserResponse>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by("createTime").descending());
        return Result.success(userService.getUsers(keyword, deptId, status, pageable));
    }

    @GetMapping("/{id}")
    public Result<UserResponse> getUserById(@PathVariable Long id) {
        return Result.success(userService.getUserById(id));
    }

    @Log(module = "用户管理", action = "新建用户")
    @PostMapping
    @PreAuthorize("hasAuthority('user:edit')")
    public Result<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return Result.success(userService.createUser(request));
    }

    @Log(module = "用户管理", action = "修改用户")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:edit')")
    public Result<UserResponse> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        return Result.success(userService.updateUser(id, request));
    }

    @Log(module = "用户管理", action = "删除用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:edit')")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('user:edit')")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        userService.updateStatus(id, body.get("status"));
        return Result.success();
    }
}
