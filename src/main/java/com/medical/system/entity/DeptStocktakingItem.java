package com.medical.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "dept_stocktaking_items")
public class DeptStocktakingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stocktaking_id", nullable = false)
    private Long stocktakingId;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(name = "system_quantity", nullable = false)
    private BigDecimal systemQuantity = BigDecimal.ZERO;

    @Column(name = "actual_quantity")
    private BigDecimal actualQuantity;

    private BigDecimal consumption;

    private String note;
}
