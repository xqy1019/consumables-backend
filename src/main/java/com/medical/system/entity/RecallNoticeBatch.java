package com.medical.system.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "recall_notice_batches")
public class RecallNoticeBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recall_id")
    private Long recallId;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    /** null 表示该耗材所有批次均被召回 */
    @Column(name = "batch_number")
    private String batchNumber;

    @Column(name = "quantity_affected")
    private Integer quantityAffected;

    private String remark;
}
