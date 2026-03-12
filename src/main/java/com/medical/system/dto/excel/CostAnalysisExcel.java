package com.medical.system.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class CostAnalysisExcel {
    @ExcelProperty("月份")
    private String month;
    @ExcelProperty("耗材类别")
    private String category;
    @ExcelProperty("成本(元)")
    private Double cost;
}
