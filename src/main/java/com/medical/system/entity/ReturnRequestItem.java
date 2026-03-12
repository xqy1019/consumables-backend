package com.medical.system.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "return_request_items")
public class ReturnRequestItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "return_id")
    private Long returnId;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(name = "batch_number")
    private String batchNumber;

    private Integer quantity;

    private String remark;
}
