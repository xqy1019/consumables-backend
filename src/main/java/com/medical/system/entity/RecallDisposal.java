package com.medical.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "recall_disposals")
public class RecallDisposal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recall_id", nullable = false)
    private Long recallId;

    @Column(name = "inventory_id")
    private Long inventoryId;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(name = "batch_number")
    private String batchNumber;

    private Integer quantity;

    /** RETURN=退货 / DESTROY=销毁 / QUARANTINE=隔离封存 */
    @Column(name = "disposal_type", nullable = false)
    private String disposalType;

    @Column(name = "disposal_date")
    private LocalDateTime disposalDate;

    private String remark;

    @Column(name = "operator_id")
    private Long operatorId;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}
