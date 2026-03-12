package com.medical.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ai_expiry_disposal_cache")
public class AiExpiryDisposalCache {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inventory_id", nullable = false)
    private Long inventoryId;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(nullable = false, length = 20)
    private String action; // ACCELERATE/TRANSFER/RETURN/DAMAGE

    @Column(length = 100)
    private String reason;

    @Column(name = "days_left")
    private Integer daysLeft;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @PrePersist
    public void prePersist() {
        if (generatedAt == null) {
            generatedAt = LocalDateTime.now();
        }
    }
}
