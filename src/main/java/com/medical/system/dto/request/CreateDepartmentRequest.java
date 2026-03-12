package com.medical.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateDepartmentRequest {
    @NotBlank(message = "科室名称不能为空")
    private String deptName;
    @NotBlank(message = "科室编码不能为空")
    private String deptCode;
    private Long parentId;
    private Integer level = 2;
    private String description;
}
