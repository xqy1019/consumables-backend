package com.medical.system.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PriceCheckResult {
    private Long materialId;
    private String materialName;
    private BigDecimal currentPrice;
    private BigDecimal avgHistoricalPrice;
    private String status; // NORMAL / ABNORMAL_HIGH / ABNORMAL_LOW
    private Double deviation; // 偏差百分比
    private String reason;
}
