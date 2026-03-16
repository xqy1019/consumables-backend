package com.medical.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "dept_inventory", uniqueConstraints = @UniqueConstraint(columnNames = {"dept_id", "material_id"}))
public class DeptInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dept_id", nullable = false)
    private Long deptId;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(name = "current_quantity", nullable = false)
    private BigDecimal currentQuantity = BigDecimal.ZERO;

    @Column(name = "last_stocktaking_at")
    private LocalDateTime lastStocktakingAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
