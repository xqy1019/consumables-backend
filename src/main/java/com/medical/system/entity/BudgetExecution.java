package com.medical.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "budget_executions")
public class BudgetExecution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "requisition_id")
    private Long requisitionId;

    @Column(name = "dept_id", nullable = false)
    private Long deptId;

    @Column(nullable = false)
    private BigDecimal amount;

    private String description;

    @CreationTimestamp
    @Column(name = "create_time")
    private LocalDateTime createTime;
}
