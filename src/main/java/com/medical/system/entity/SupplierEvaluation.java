package com.medical.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "supplier_evaluations")
public class SupplierEvaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "eval_year", nullable = false)
    private Integer evalYear;

    @Column(name = "eval_quarter", nullable = false)
    private Integer evalQuarter;

    @Column(name = "price_score")
    private BigDecimal priceScore;

    @Column(name = "quality_score")
    private BigDecimal qualityScore;

    @Column(name = "delivery_score")
    private BigDecimal deliveryScore;

    @Column(name = "service_score")
    private BigDecimal serviceScore;

    @Column(name = "total_score")
    private BigDecimal totalScore;

    private String grade; // A/B/C/D

    @Column(name = "quality_rate")
    private BigDecimal qualityRate;

    @Column(name = "delivery_rate")
    private BigDecimal deliveryRate;

    @Column(name = "avg_price_ratio")
    private BigDecimal avgPriceRatio;

    private String remark;

    @Column(name = "evaluated_by")
    private Long evaluatedBy;

    @CreationTimestamp
    @Column(name = "create_time")
    private LocalDateTime createTime;
}
