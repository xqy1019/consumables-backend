package com.medical.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "budget_plans")
public class BudgetPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dept_id", nullable = false)
    private Long deptId;

    @Column(nullable = false)
    private Integer year;

    private Integer quarter; // null=年度预算, 1-4=季度预算

    private String category; // 耗材分类，null=不限

    @Column(name = "budget_amount", nullable = false)
    private BigDecimal budgetAmount;

    @Column(name = "used_amount")
    private BigDecimal usedAmount = BigDecimal.ZERO;

    private String status = "ACTIVE";

    private String remark;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "create_time")
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
