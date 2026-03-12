package com.medical.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "materials")
public class Material {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "material_code", unique = true, nullable = false)
    private String materialCode;

    @Column(name = "material_name", nullable = false)
    private String materialName;

    private String category;
    private String specification;
    private String unit;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "standard_price")
    private BigDecimal standardPrice;

    @Column(name = "min_stock")
    private Integer minStock = 0;

    @Column(name = "max_stock")
    private Integer maxStock = 1000;

    @Column(name = "lead_time")
    private Integer leadTime = 7;

    private String description;

    @Column(name = "registration_no")
    private String registrationNo;

    @Column(name = "registration_expiry")
    private java.time.LocalDate registrationExpiry;

    private String manufacturer;

    @Column(name = "is_high_value")
    private Boolean isHighValue = false;

    private Integer status = 1;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
