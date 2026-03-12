package com.medical.system.dto.response;

import lombok.Data;

@Data
public class PermissionResponse {
    private Long id;
    private String permissionCode;
    private String permissionName;
    private String type;
    private String description;
    private Integer sortOrder;
}
