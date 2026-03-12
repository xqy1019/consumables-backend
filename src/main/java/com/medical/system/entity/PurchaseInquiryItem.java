package com.medical.system.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "purchase_inquiry_item")
public class PurchaseInquiryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inquiry_id", nullable = false)
    private Long inquiryId;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    private Integer quantity;

    @Column(name = "quoted_price")
    private BigDecimal quotedPrice;

    private BigDecimal subtotal;

    @Column(name = "delivery_days")
    private Integer deliveryDays;

    private String remark;
}
