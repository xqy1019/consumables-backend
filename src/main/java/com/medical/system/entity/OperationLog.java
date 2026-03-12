package com.medical.system.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "operation_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String username;
    private String deptName;
    private String module;
    private String action;

    @Column(length = 500)
    private String requestUrl;
    private String requestMethod;

    @Column(columnDefinition = "TEXT")
    private String requestParams;

    private Integer responseCode;
    private String ipAddr;
    private Integer status; // 1=成功, 0=失败

    @Column(columnDefinition = "TEXT")
    private String errorMsg;

    private Long durationMs;

    @Column(name = "operate_time")
    private LocalDateTime operateTime;
}
