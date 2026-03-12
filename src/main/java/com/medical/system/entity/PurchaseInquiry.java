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
@Table(name = "purchase_inquiry")
public class PurchaseInquiry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inquiry_no", unique = true, nullable = false)
    private String inquiryNo;

    @Column(name = "req_id")
    private Long reqId;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "inquiry_date")
    private LocalDateTime inquiryDate;

    @Column(name = "valid_date")
    private LocalDate validDate;

    private String status = "SENT";

    @Column(name = "total_amount")
    private BigDecimal totalAmount = BigDecimal.ZERO;

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
