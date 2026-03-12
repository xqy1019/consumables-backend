package com.medical.system.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateRequisitionRequest {
    @NotNull(message = "科室ID不能为空")
    private Long deptId;
    private LocalDate requiredDate;
    private String remark;
    @NotEmpty(message = "申领项目不能为空")
    private List<ItemRequest> items;

    @Data
    public static class ItemRequest {
        @NotNull(message = "耗材ID不能为空")
        private Long materialId;
        @NotNull(message = "申领数量不能为空")
        @Min(1)
        private Integer quantity;
        private String remark;
    }
}
