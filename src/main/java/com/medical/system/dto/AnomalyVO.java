package com.medical.system.dto;

import lombok.Data;

@Data
public class AnomalyVO {
    private Long materialId;
    private String materialName;
    private String materialCode;
    private Long deptId;
    private String deptName;
    private String anomalyDate;
    private Integer anomalyQuantity;
    private Integer avgDailyConsumption;
    private Double anomalyRatio;
    private String severity;   // HIGH / MEDIUM
    private String reason;
}
