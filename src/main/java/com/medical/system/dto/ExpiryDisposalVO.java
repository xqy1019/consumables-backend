package com.medical.system.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ExpiryDisposalVO {
    private Long inventoryId;
    private String materialName;
    private String materialCode;
    private String batchNumber;
    private Integer quantity;
    private LocalDate expiryDate;
    private Integer daysLeft;
    private Integer dailyConsumption;
    private Integer estimatedConsumable;
    private Integer riskQuantity;
    private String advice;   // ACCELERATE / TRANSFER / RETURN / DAMAGE
    private String reason;
}
