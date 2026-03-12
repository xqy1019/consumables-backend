package com.medical.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_dict_item")
public class SysDictItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dict_id", nullable = false)
    private Long dictId;

    @Column(name = "item_label", nullable = false)
    private String itemLabel;

    @Column(name = "item_value", nullable = false)
    private String itemValue;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    private String remark;
    private Integer status = 1;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
