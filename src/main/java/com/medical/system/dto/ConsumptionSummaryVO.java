package com.medical.system.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ConsumptionSummaryVO {
    private Long deptId;
    private String deptName;
    private Long materialId;
    private String materialName;
    private String specification;
    private String unit;
    private BigDecimal requisitionQuantity;
    private BigDecimal stocktakingConsumption;
    private BigDecimal estimatedConsumption;
    private String source;
    private String yearMonth;
}
