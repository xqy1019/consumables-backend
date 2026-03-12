package com.medical.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ai_prediction_result")
public class AiPredictionResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(name = "dept_id")
    private Long deptId;

    @Column(name = "prediction_month", nullable = false)
    private String predictionMonth;

    @Column(name = "predicted_quantity")
    private Integer predictedQuantity;

    @Column(name = "actual_quantity")
    private Integer actualQuantity;

    private BigDecimal confidence;
    private String algorithm = "MA3";

    @Column(name = "accuracy_rate")
    private BigDecimal accuracyRate;

    @Column(name = "evaluated_at")
    private LocalDateTime evaluatedAt;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}
