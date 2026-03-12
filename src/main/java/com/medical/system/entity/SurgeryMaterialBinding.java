package com.medical.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "surgery_material_binding")
public class SurgeryMaterialBinding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "surgery_id", nullable = false)
    private Long surgeryId;

    @Column(name = "udi_id")
    private Long udiId;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    private Integer quantity = 1;

    @Column(name = "use_date")
    private LocalDateTime useDate;

    private String remark;
}
