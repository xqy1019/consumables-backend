package com.medical.system.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class MonthlyReportParSuggestionExcel {
    @ExcelProperty("科室")
    private String deptName;
    @ExcelProperty("耗材名称")
    private String materialName;
    @ExcelProperty("单位")
    private String unit;
    @ExcelProperty("调整方向")
    private String direction;
    @ExcelProperty("当前定数")
    private Integer currentPar;
    @ExcelProperty("建议定数")
    private Integer suggestedPar;
    @ExcelProperty("当前最低")
    private Integer currentMin;
    @ExcelProperty("建议最低")
    private Integer suggestedMin;
    @ExcelProperty("月均消耗")
    private Double avgMonthlyUsage;
    @ExcelProperty("原因")
    private String reason;
}
