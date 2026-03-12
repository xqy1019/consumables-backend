package com.medical.system.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class MaterialResponse {
    private Long id;
    private String materialCode;
    private String materialName;
    private String category;
    private String specification;
    private String unit;
    private Long supplierId;
    private String supplierName;
    private BigDecimal standardPrice;
    private Integer minStock;
    private Integer maxStock;
    private Integer leadTime;
    private Integer currentStock;
    private String description;
    private String registrationNo;
    private LocalDate registrationExpiry;
    private String manufacturer;
    private Boolean isHighValue;
    private Integer status;
    private LocalDateTime createTime;
}
