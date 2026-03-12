package com.medical.system.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreateInquiryRequest {
    @NotNull(message = "供应商ID不能为空")
    private Long supplierId;
    private Long reqId;
    private LocalDate validDate;
    private String remark;
    private List<ItemRequest> items;

    @Data
    public static class ItemRequest {
        @NotNull(message = "耗材ID不能为空")
        private Long materialId;
        @NotNull(message = "数量不能为空")
        private Integer quantity;
        private BigDecimal quotedPrice;
        private Integer deliveryDays;
    }
}
