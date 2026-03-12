package com.medical.system.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RequisitionResponse {
    private Long id;
    private String requisitionNo;
    private Long deptId;
    private String deptName;
    private LocalDateTime requisitionDate;
    private LocalDate requiredDate;
    private String status;
    private String statusLabel;
    private String remark;
    private Long createdBy;
    private String createdByName;
    private Long signedBy;
    private String signedByName;
    private LocalDateTime signTime;
    private String signRemark;
    private List<ItemResponse> items;
    private List<ApprovalRecordResponse> approvalRecords;
    private LocalDateTime createTime;

    @Data
    public static class ItemResponse {
        private Long id;
        private Long materialId;
        private String materialName;
        private String specification;
        private String unit;
        private Integer quantity;
        private Integer actualQuantity;
        private String remark;
    }

    @Data
    public static class ApprovalRecordResponse {
        private Long id;
        private Long approverId;
        private String approverName;
        private LocalDateTime approvalTime;
        private String status;
        private String remark;
    }
}
