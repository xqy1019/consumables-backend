package com.medical.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "inventory_damage")
public class InventoryDamage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "damage_no", unique = true, nullable = false)
    private String damageNo;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(name = "inventory_id", nullable = false)
    private Long inventoryId;

    @Column(name = "batch_number")
    private String batchNumber;

    private Integer quantity;

    @Column(name = "damage_reason")
    private String damageReason;

    @Column(name = "damage_date")
    private LocalDateTime damageDate;

    private String status = "CONFIRMED";
    private String remark;

    @Column(name = "operator_id")
    private Long operatorId;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}
