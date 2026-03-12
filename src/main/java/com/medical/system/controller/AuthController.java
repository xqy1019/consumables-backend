package com.medical.system.controller;

import com.medical.system.annotation.Log;
import com.medical.system.common.Result;
import com.medical.system.dto.request.LoginRequest;
import com.medical.system.dto.response.LoginResponse;
import com.medical.system.dto.response.UserResponse;
import com.medical.system.service.impl.AuthServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthServiceImpl authService;

    @Log(module = "认证", action = "用户登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @GetMapping("/me")
    public Result<UserResponse> getCurrentUser(Authentication authentication) {
        return Result.success(authService.getCurrentUser(authentication.getName()));
    }
}
