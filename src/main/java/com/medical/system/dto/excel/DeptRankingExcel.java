package com.medical.system.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class DeptRankingExcel {
    @ExcelProperty("科室名称")
    private String deptName;
    @ExcelProperty("使用量")
    private Long quantity;
    @ExcelProperty("金额(元)")
    private Double amount;
    @ExcelProperty("排名")
    private Integer rank;
}
