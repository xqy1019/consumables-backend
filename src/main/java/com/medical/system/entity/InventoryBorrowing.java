package com.medical.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "inventory_borrowing")
public class InventoryBorrowing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "borrowing_no", unique = true, nullable = false)
    private String borrowingNo;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(name = "inventory_id", nullable = false)
    private Long inventoryId;

    @Column(name = "batch_number")
    private String batchNumber;

    private Integer quantity;

    @Column(name = "dept_id")
    private Long deptId;

    @Column(name = "borrower_name")
    private String borrowerName;

    @Column(name = "borrowing_date")
    private LocalDateTime borrowingDate;

    @Column(name = "expected_return_date")
    private LocalDate expectedReturnDate;

    @Column(name = "actual_return_date")
    private LocalDate actualReturnDate;

    private String status = "BORROWED";
    private String remark;

    @Column(name = "operator_id")
    private Long operatorId;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
