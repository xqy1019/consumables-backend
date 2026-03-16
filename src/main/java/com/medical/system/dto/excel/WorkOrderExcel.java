package com.medical.system.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class WorkOrderExcel {
    @ExcelProperty("工单ID")
    private Long id;
    @ExcelProperty("科室")
    private String deptName;
    @ExcelProperty("耗材名称")
    private String materialName;
    @ExcelProperty("异常类型")
    private String anomalyType;
    @ExcelProperty("偏差率(%)")
    private String deviationRate;
    @ExcelProperty("优先级")
    private String priority;
    @ExcelProperty("状态")
    private String status;
    @ExcelProperty("负责人")
    private String assignedToName;
    @ExcelProperty("创建人")
    private String createdByName;
    @ExcelProperty("创建时间")
    private String createdAt;
    @ExcelProperty("解决方案")
    private String resolution;
}
