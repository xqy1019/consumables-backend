package com.medical.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "inventory_transfer")
public class InventoryTransfer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transfer_no", unique = true, nullable = false)
    private String transferNo;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(name = "inventory_id", nullable = false)
    private Long inventoryId;

    private Integer quantity;

    @Column(name = "from_location")
    private String fromLocation;

    @Column(name = "to_location", nullable = false)
    private String toLocation;

    @Column(name = "transfer_date")
    private LocalDateTime transferDate;

    private String status = "COMPLETED";
    private String remark;

    @Column(name = "operator_id")
    private Long operatorId;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}
