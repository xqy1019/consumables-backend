package com.medical.system.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "inventory_stocktaking_item")
public class InventoryStocktakingItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stocktaking_id", nullable = false)
    private Long stocktakingId;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(name = "inventory_id")
    private Long inventoryId;

    @Column(name = "batch_number")
    private String batchNumber;

    @Column(name = "system_quantity")
    private Integer systemQuantity = 0;

    @Column(name = "actual_quantity")
    private Integer actualQuantity;

    private Integer difference;
    private String remark;
}
