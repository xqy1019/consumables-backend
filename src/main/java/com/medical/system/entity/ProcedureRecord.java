package com.medical.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "procedure_records")
public class ProcedureRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dept_id", nullable = false)
    private Long deptId;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "performed_by")
    private Long performedBy;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;

    /** 本次执行操作次数（默认1次） */
    @Column
    private Integer quantity = 1;

    @Column(name = "patient_info", length = 100)
    private String patientInfo;

    @Column(columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
