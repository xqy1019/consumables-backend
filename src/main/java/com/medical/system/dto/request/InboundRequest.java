package com.medical.system.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class InboundRequest {
    @NotNull(message = "耗材ID不能为空")
    private Long materialId;
    private String batchNumber;
    @NotNull(message = "入库数量不能为空")
    @Min(value = 1, message = "入库数量不能小于1")
    private Integer quantity;
    private String location;
    private LocalDate manufactureDate;
    private LocalDate expiryDate;
    private Long supplierId;
    private LocalDate receiveDate;
    private String remark;
    /** PASSED（默认）/ PENDING（待验收）/ REJECTED（验收不合格） */
    private String inspectionStatus = "PASSED";
    private String inspectionRemark;
    private Long inspectorId;
}
