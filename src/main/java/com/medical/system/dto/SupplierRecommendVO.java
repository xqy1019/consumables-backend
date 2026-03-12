package com.medical.system.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SupplierRecommendVO {
    private Long supplierId;
    private String supplierName;
    private BigDecimal avgPrice;
    private Double qualityRate;
    private Integer score; // 综合评分 0-100
    private String reason;
}
