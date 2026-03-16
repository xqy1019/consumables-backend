package com.medical.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "dept_par_levels")
public class DeptParLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dept_id", nullable = false)
    private Long deptId;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    /** 定数（标准库存量） */
    @Column(name = "par_quantity", nullable = false)
    private BigDecimal parQuantity = BigDecimal.ZERO;

    /** 最低库存量，低于此值触发补货预警 */
    @Column(name = "min_quantity", nullable = false)
    private BigDecimal minQuantity = BigDecimal.ZERO;

    /** 月度领用限额，为空表示不限 */
    @Column(name = "monthly_limit")
    private BigDecimal monthlyLimit;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
