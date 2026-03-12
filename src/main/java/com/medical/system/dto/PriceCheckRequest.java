package com.medical.system.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PriceCheckRequest {
    private Long materialId;
    private String materialName;
    private BigDecimal currentPrice;
    private Integer quantity;
}
