package com.medical.system.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;

public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final Long deptId;
    private final String username;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    private static final Set<String> ALL_DEPT_ROLES =
            Set.of("ROLE_ADMIN", "ROLE_WAREHOUSE_KEEPER", "ROLE_PURCHASER", "ROLE_FINANCE");

    public CustomUserDetails(Long userId, Long deptId, String username, String password,
                             Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.deptId = deptId;
        this.username = username;
        this.password = password;
        this.authorities = authorities;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getDeptId() {
        return deptId;
    }

    /** 判断是否为跨科室角色（ADMIN / WAREHOUSE_KEEPER / PURCHASER / FINANCE） */
    public boolean canAccessAllDepts() {
        return authorities.stream()
                .anyMatch(a -> ALL_DEPT_ROLES.contains(a.getAuthority()));
    }

    public boolean isAdmin() {
        return authorities.stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
