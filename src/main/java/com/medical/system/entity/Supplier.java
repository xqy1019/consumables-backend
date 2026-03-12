package com.medical.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "suppliers")
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_name", nullable = false)
    private String supplierName;

    @Column(name = "supplier_code", unique = true, nullable = false)
    private String supplierCode;

    @Column(name = "contact_person")
    private String contactPerson;

    private String phone;
    private String email;
    private String address;

    @Column(name = "license_no")
    private String licenseNo;

    @Column(name = "license_expiry")
    private java.time.LocalDate licenseExpiry;

    private Integer status = 1;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
