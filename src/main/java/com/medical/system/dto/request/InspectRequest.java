package com.medical.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InspectRequest {
    @NotBlank(message = "验收结果不能为空")
    private String inspectionStatus; // PASSED / REJECTED
    private String inspectionRemark;
}
