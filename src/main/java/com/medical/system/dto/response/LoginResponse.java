package com.medical.system.dto.response;

import lombok.Data;

import java.util.Set;

@Data
public class LoginResponse {
    private String token;
    private String tokenType;
    private Long userId;
    private String username;
    private String realName;
    private Set<String> roles;
    private Set<String> permissions;

    public LoginResponse(String token, Long userId, String username, String realName,
                         Set<String> roles, Set<String> permissions) {
        this.token = token;
        this.tokenType = "Bearer";
        this.userId = userId;
        this.username = username;
        this.realName = realName;
        this.roles = roles;
        this.permissions = permissions;
    }
}
