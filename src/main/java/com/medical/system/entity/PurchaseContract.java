package com.medical.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "purchase_contract")
public class PurchaseContract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contract_no", unique = true, nullable = false)
    private String contractNo;

    @Column(name = "inquiry_id")
    private Long inquiryId;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "contract_date", nullable = false)
    private LocalDate contractDate;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    private String status = "ACTIVE";
    private String remark;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
