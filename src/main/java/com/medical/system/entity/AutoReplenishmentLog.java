package com.medical.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "auto_replenishment_log")
public class AutoReplenishmentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dept_id", nullable = false)
    private Long deptId;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(name = "current_quantity", nullable = false)
    private BigDecimal currentQuantity;

    @Column(name = "min_quantity", nullable = false)
    private BigDecimal minQuantity;

    @Column(name = "replenish_quantity", nullable = false)
    private BigDecimal replenishQuantity;

    @Column(name = "requisition_id")
    private Long requisitionId;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
