package com.medical.system.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DepartmentResponse {
    private Long id;
    private String deptName;
    private String deptCode;
    private Long parentId;
    private String parentName;
    private Integer level;
    private String description;
    private Integer status;
    private LocalDateTime createTime;
    private List<DepartmentResponse> children;
}
