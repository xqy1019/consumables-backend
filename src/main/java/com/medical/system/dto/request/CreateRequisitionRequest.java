package com.medical.system.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateRequisitionRequest {
    @NotNull(message = "科室ID不能为空")
    private Long deptId;
    private LocalDate requiredDate;
    @Size(max = 500, message = "备注不能超过500个字符")
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
