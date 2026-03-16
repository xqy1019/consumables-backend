package com.medical.system.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class MonthlyReportAnomalyExcel {
    @ExcelProperty("科室")
    private String deptName;
    @ExcelProperty("耗材名称")
    private String materialName;
    @ExcelProperty("单位")
    private String unit;
    @ExcelProperty("异常级别")
    private String level;
    @ExcelProperty("偏差率(%)")
    private String deviationRate;
    @ExcelProperty("当月用量")
    private Integer currentUsage;
    @ExcelProperty("历史月均")
    private Double historicalAvg;
    @ExcelProperty("月限额")
    private String monthlyLimit;
    @ExcelProperty("是否超限")
    private String overLimit;
}
