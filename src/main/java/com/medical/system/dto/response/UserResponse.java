package com.medical.system.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class UserResponse {
    private Long id;
    private String username;
    private String realName;
    private String email;
    private String phone;
    private Long deptId;
    private String deptName;
    private Integer status;
    private Set<RoleResponse> roles;
    private LocalDateTime createTime;
}
