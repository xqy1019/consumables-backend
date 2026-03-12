package com.medical.system.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class InventoryResponse {
    private Long id;
    private Long materialId;
    private String materialName;
    private String materialCode;
    private String specification;
    private String unit;
    private String batchNumber;
    private Integer quantity;
    private String location;
    private LocalDate manufactureDate;
    private LocalDate expiryDate;
    private Long supplierId;
    private String supplierName;
    private LocalDate receiveDate;
    private String inspectionStatus;
    private String inspectionRemark;
    private LocalDateTime inspectTime;
    private Integer status;
    private boolean expiring;
    private boolean lowStock;
    private LocalDateTime createTime;
}
