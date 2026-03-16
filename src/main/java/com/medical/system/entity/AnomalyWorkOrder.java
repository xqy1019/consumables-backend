package com.medical.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "anomaly_work_orders")
public class AnomalyWorkOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dept_id", nullable = false)
    private Long deptId;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(name = "anomaly_type", nullable = false, length = 30)
    private String anomalyType;

    @Column(name = "deviation_rate")
    private Double deviationRate;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false, length = 20)
    private String priority;

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(length = 1000)
    private String resolution;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "sla_breached", nullable = false)
    private Boolean slaBreached = false;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
