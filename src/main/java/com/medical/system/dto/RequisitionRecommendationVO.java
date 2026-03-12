package com.medical.system.dto;

import lombok.Data;

@Data
public class RequisitionRecommendationVO {
    private Long materialId;
    private String materialName;
    private String specification;
    private String unit;
    private Integer recommendedQuantity;  // 最终推荐数量
    private Integer currentStock;         // 当前库存
    private Integer predictedConsumption; // 预测下月消耗
    private String reason;                // Claude 推荐理由
    private String urgency;               // HIGH / MEDIUM / LOW
}
