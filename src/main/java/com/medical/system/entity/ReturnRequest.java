package com.medical.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "return_requests")
public class ReturnRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "return_no", unique = true, nullable = false)
    private String returnNo;

    @Column(name = "dept_id")
    private Long deptId;

    /** PENDING / APPROVED / COMPLETED / REJECTED */
    private String status = "PENDING";

    private String remark;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_time")
    private LocalDateTime approvedTime;

    // 子记录通过 ReturnRequestItemRepository 手动管理，避免 Hibernate 单向 @JoinColumn NOT NULL 问题

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
