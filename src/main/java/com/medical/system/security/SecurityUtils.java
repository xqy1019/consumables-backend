package com.medical.system.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    private SecurityUtils() {}

    public static CustomUserDetails getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails cu) {
            return cu;
        }
        throw new IllegalStateException("未找到当前登录用户信息");
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

    public static Long getCurrentDeptId() {
        return getCurrentUser().getDeptId();
    }

    public static boolean isAdmin() {
        return getCurrentUser().isAdmin();
    }

    public static boolean canAccessAllDepts() {
        return getCurrentUser().canAccessAllDepts();
    }
}
