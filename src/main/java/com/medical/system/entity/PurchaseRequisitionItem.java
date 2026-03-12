package com.medical.system.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "purchase_requisition_item")
public class PurchaseRequisitionItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "req_id", nullable = false)
    private Long reqId;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    private Integer quantity;

    @Column(name = "estimated_price")
    private BigDecimal estimatedPrice;

    private BigDecimal subtotal;
    private String remark;
}
