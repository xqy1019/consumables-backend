package com.medical.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "purchase_requisition")
public class PurchaseRequisition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "req_no", unique = true, nullable = false)
    private String reqNo;

    @Column(name = "dept_id")
    private Long deptId;

    @Column(name = "req_date")
    private LocalDateTime reqDate;

    @Column(name = "required_date")
    private LocalDate requiredDate;

    private String status = "DRAFT";

    @Column(name = "total_amount")
    private BigDecimal totalAmount = BigDecimal.ZERO;

    private String remark;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_time")
    private LocalDateTime approvedTime;

    @Column(name = "approval_remark")
    private String approvalRemark;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
