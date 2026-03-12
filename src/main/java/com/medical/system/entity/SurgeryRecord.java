package com.medical.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "surgery_record")
public class SurgeryRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "surgery_no", unique = true, nullable = false)
    private String surgeryNo;

    @Column(name = "patient_id")
    private String patientId;

    @Column(name = "patient_name", nullable = false)
    private String patientName;

    @Column(name = "patient_age")
    private Integer patientAge;

    @Column(name = "patient_gender")
    private String patientGender;

    @Column(name = "dept_id")
    private Long deptId;

    @Column(name = "surgery_date", nullable = false)
    private LocalDateTime surgeryDate;

    @Column(name = "surgery_type")
    private String surgeryType;

    @Column(name = "doctor_name")
    private String doctorName;

    private String status = "COMPLETED";
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
