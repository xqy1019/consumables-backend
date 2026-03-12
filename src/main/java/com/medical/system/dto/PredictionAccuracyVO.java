package com.medical.system.dto;

import lombok.Data;
import java.util.List;

@Data
public class PredictionAccuracyVO {
    private String month;
    private Double avgAccuracyRate;
    private Integer evaluatedCount;
    private List<MaterialAccuracy> worstMaterials; // 最差Top10

    @Data
    public static class MaterialAccuracy {
        private Long materialId;
        private String materialName;
        private Integer predictedQuantity;
        private Integer actualQuantity;
        private Double accuracyRate;
    }
}
