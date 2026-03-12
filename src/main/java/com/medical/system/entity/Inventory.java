package com.medical.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "inventory")
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(name = "batch_number", nullable = false)
    private String batchNumber;

    private Integer quantity = 0;
    private String location;

    @Column(name = "manufacture_date")
    private LocalDate manufactureDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "receive_date")
    private LocalDate receiveDate;

    // PASSED / PENDING / REJECTED
    @Column(name = "inspection_status")
    private String inspectionStatus = "PASSED";

    @Column(name = "inspection_remark")
    private String inspectionRemark;

    @Column(name = "inspector_id")
    private Long inspectorId;

    @Column(name = "inspect_time")
    private LocalDateTime inspectTime;

    private Integer status = 1;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
