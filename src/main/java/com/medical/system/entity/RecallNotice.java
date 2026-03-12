package com.medical.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "recall_notices")
public class RecallNotice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recall_no", unique = true, nullable = false)
    private String recallNo;

    @Column(nullable = false)
    private String title;

    @Column(name = "recall_reason")
    private String recallReason;

    /** I=最严重 / II=中等 / III=轻微 */
    @Column(name = "recall_level")
    private String recallLevel = "II";

    /** SUPPLIER=供应商主动召回 / REGULATOR=监管部门强制召回 */
    private String source = "SUPPLIER";

    @Column(name = "issued_date")
    private LocalDate issuedDate;

    /** ACTIVE=进行中 / CLOSED=已关闭 */
    private String status = "ACTIVE";

    private String remark;

    @Column(name = "created_by")
    private Long createdBy;

    // 子记录通过 RecallNoticeBatchRepository 手动管理，避免 Hibernate 单向 @JoinColumn NOT NULL 问题

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
