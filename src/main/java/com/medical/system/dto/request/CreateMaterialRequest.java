package com.medical.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateMaterialRequest {
    @NotBlank(message = "耗材编码不能为空")
    private String materialCode;
    @NotBlank(message = "耗材名称不能为空")
    private String materialName;
    private String category;
    private String specification;
    private String unit;
    private Long supplierId;
    private BigDecimal standardPrice;
    private Integer minStock = 0;
    private Integer maxStock = 1000;
    private Integer leadTime = 7;
    private String description;
    private String registrationNo;
    private LocalDate registrationExpiry;
    private String manufacturer;
    private Boolean isHighValue = false;
}
