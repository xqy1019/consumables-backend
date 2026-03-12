package com.medical.system.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class DashboardResponse {
    private Long totalMaterials;
    private Long totalInventoryItems;
    private Long expiringAlerts;
    private Long lowStockAlerts;
    private Long pendingRequisitions;
    private Long totalRequisitions;
    private List<RecentActivity> recentActivities;
    private List<TrendPoint> weeklyTrend;
    private List<CategoryDistribution> categoryDistribution;

    @Data
    public static class RecentActivity {
        private String type;
        private String description;
        private String time;
        private String operator;
    }

    @Data
    public static class TrendPoint {
        private String date;
        private Long inbound;
        private Long outbound;
    }

    @Data
    public static class CategoryDistribution {
        private String name;
        private Long value;
    }
}
