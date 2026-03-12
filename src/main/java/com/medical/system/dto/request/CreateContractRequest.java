package com.medical.system.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreateContractRequest {
    @NotNull(message = "供应商ID不能为空")
    private Long supplierId;
    @NotNull(message = "合同日期不能为空")
    private LocalDate contractDate;
    private LocalDate deliveryDate;
    private Long inquiryId;
    private String remark;
    @NotEmpty(message = "合同项目不能为空")
    private List<ItemRequest> items;

    @Data
    public static class ItemRequest {
        @NotNull(message = "耗材ID不能为空")
        private Long materialId;
        @NotNull(message = "数量不能为空")
        private Integer quantity;
        @NotNull(message = "单价不能为空")
        private BigDecimal unitPrice;
    }
}
