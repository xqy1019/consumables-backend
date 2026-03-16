package com.medical.system.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "procedure_template_items")
public class ProcedureTemplateItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(length = 200)
    private String note;
}
