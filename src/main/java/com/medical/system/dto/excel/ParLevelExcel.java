package com.medical.system.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class ParLevelExcel {
    @ExcelProperty("科室")
    private String deptName;
    @ExcelProperty("耗材名称")
    private String materialName;
    @ExcelProperty("规格")
    private String specification;
    @ExcelProperty("单位")
    private String unit;
    @ExcelProperty("定数量(PAR)")
    private Integer parQuantity;
    @ExcelProperty("最低库存")
    private Integer minQuantity;
}
