package com.medical.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "inventory_transactions")
public class InventoryTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(name = "transaction_type", nullable = false)
    private String transactionType; // INBOUND, OUTBOUND

    private Integer quantity;

    @Column(name = "batch_number")
    private String batchNumber;

    @Column(name = "dept_id")
    private Long deptId;

    @Column(name = "requisition_id")
    private Long requisitionId;

    @Column(name = "operator_id")
    private Long operatorId;

    private String remark;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}
