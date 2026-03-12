package com.medical.system.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class ConsumptionTrendExcel {
    @ExcelProperty("月份")
    private String month;
    @ExcelProperty("使用量")
    private Long quantity;
    @ExcelProperty("金额(元)")
    private Double amount;
}
