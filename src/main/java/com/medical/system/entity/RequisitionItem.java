package com.medical.system.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "requisition_items")
public class RequisitionItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requisition_id")
    private Long requisitionId;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    private Integer quantity;

    @Column(name = "actual_quantity")
    private Integer actualQuantity;

    private String remark;
}
