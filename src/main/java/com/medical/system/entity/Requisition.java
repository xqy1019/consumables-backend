package com.medical.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "requisitions")
public class Requisition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requisition_no", unique = true, nullable = false)
    private String requisitionNo;

    @Column(name = "dept_id", nullable = false)
    private Long deptId;

    @Column(name = "requisition_date")
    private LocalDateTime requisitionDate;

    @Column(name = "required_date")
    private LocalDate requiredDate;

    // DRAFT, PENDING, APPROVED, REJECTED, DISPATCHED
    private String status = "DRAFT";

    private String remark;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "signed_by")
    private Long signedBy;

    @Column(name = "sign_time")
    private LocalDateTime signTime;

    @Column(name = "sign_remark")
    private String signRemark;

    // 子记录通过 RequisitionItemRepository 手动管理，避免 Hibernate 单向 @JoinColumn NOT NULL 问题

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
