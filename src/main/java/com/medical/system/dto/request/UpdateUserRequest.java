package com.medical.system.dto.request;

import lombok.Data;

import java.util.Set;

@Data
public class UpdateUserRequest {
    private String realName;
    private String email;
    private String phone;
    private Long deptId;
    private Set<Long> roleIds;
}
