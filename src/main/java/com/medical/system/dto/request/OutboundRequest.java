package com.medical.system.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OutboundRequest {
    @NotNull(message = "库存ID不能为空")
    private Long inventoryId;
    @NotNull(message = "出库数量不能为空")
    @Min(value = 1, message = "出库数量不能小于1")
    private Integer quantity;
    private Long deptId;
    private Long requisitionId;
    private String remark;
}
