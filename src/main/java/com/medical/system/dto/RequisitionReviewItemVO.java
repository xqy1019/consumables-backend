package com.medical.system.dto;

import lombok.Data;

@Data
public class RequisitionReviewItemVO {
    private Long materialId;
    private String materialName;
    private String specification;
    private String unit;
    private Integer requestedQuantity;
    private Integer avgMonthlyConsumption;
    private Integer currentStock;
    private String verdict;   // NORMAL / TOO_MUCH / TOO_LESS / ABNORMAL
    private String reason;
}
