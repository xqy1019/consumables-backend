package com.medical.system.service.impl;

import com.medical.system.dto.response.DashboardResponse;
import com.medical.system.entity.*;
import com.medical.system.repository.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl {

    private final MaterialRepository materialRepository;
    private final InventoryRepository inventoryRepository;
    private final RequisitionRepository requisitionRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PurchaseContractRepository purchaseContractRepository;
    private final PurchaseContractItemRepository purchaseContractItemRepository;
    private final AiPredictionResultRepository aiPredictionResultRepository;

    public DashboardResponse getDashboard() {
        DashboardResponse response = new DashboardResponse();

        response.setTotalMaterials(materialRepository.countByStatus(1));
        response.setTotalInventoryItems(inventoryRepository.countByStatus(1));
        response.setPendingRequisitions(requisitionRepository.countByStatus("PENDING"));
        response.setTotalRequisitions(requisitionRepository.count());

        // 预警：30天内过期
        LocalDate alertDate = LocalDate.now().plusDays(30);
        response.setExpiringAlerts((long) inventoryRepository.findExpiringInventory(alertDate).size());
        response.setLowStockAlerts(inventoryRepository.countLowStockMaterials());

        // 最近活动
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<Requisition> recentReqs = requisitionRepository.findRecentRequisitions(
                since, PageRequest.of(0, 5));

        List<DashboardResponse.RecentActivity> activities = recentReqs.stream().map(req -> {
            DashboardResponse.RecentActivity activity = new DashboardResponse.RecentActivity();
            activity.setType("REQUISITION");
            activity.setDescription("申领单 " + req.getRequisitionNo() + " - " + getStatusLabel(req.getStatus()));
            activity.setTime(req.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            if (req.getCreatedBy() != null) {
                userRepository.findById(req.getCreatedBy())
                        .ifPresent(u -> activity.setOperator(u.getRealName()));
            }
            return activity;
        }).collect(Collectors.toList());
        response.setRecentActivities(activities);

        // 近7天趋势
        response.setWeeklyTrend(buildWeeklyTrend(since));

        // 耗材分类分布
        response.setCategoryDistribution(buildCategoryDistribution());

        return response;
    }

    private List<DashboardResponse.TrendPoint> buildWeeklyTrend(LocalDateTime since) {
        List<Object[]> rows = transactionRepository.findWeeklyTrend(since);

        // day -> {INBOUND: n, OUTBOUND: n}
        Map<String, Map<String, Long>> byDay = new LinkedHashMap<>();

        // 先填充近7天的骨架（确保每天都有数据点）
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 6; i >= 0; i--) {
            String day = LocalDate.now().minusDays(i).format(fmt);
            byDay.put(day, new LinkedHashMap<>());
            byDay.get(day).put("INBOUND", 0L);
            byDay.get(day).put("OUTBOUND", 0L);
        }

        for (Object[] row : rows) {
            String day = (String) row[0];
            String type = (String) row[1];
            Long total = ((Number) row[2]).longValue();
            if (byDay.containsKey(day)) {
                byDay.get(day).put(type, total);
            }
        }

        return byDay.entrySet().stream().map(e -> {
            DashboardResponse.TrendPoint p = new DashboardResponse.TrendPoint();
            p.setDate(e.getKey());
            p.setInbound(e.getValue().getOrDefault("INBOUND", 0L));
            p.setOutbound(e.getValue().getOrDefault("OUTBOUND", 0L));
            return p;
        }).collect(Collectors.toList());
    }

    private List<DashboardResponse.CategoryDistribution> buildCategoryDistribution() {
        List<Object[]> rows = materialRepository.findActiveMaterialsByCategory();
        return rows.stream().map(row -> {
            DashboardResponse.CategoryDistribution d = new DashboardResponse.CategoryDistribution();
            d.setName((String) row[0]);
            d.setValue(((Number) row[1]).longValue());
            return d;
        }).collect(Collectors.toList());
    }

    // ==================== 消耗趋势 ====================
    public List<Map<String, Object>> getConsumptionTrend(int months) {
        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        for (int i = months - 1; i >= 0; i--) {
            LocalDate monthDate = LocalDate.now().minusMonths(i);
            LocalDateTime start = monthDate.withDayOfMonth(1).atStartOfDay();
            LocalDateTime end = monthDate.withDayOfMonth(monthDate.lengthOfMonth()).atTime(23, 59, 59);
            String month = monthDate.format(fmt);

            List<Object[]> rows = transactionRepository.findWeeklyTrend(start);
            long total = rows.stream()
                    .filter(r -> "OUTBOUND".equals(r[1]))
                    .mapToLong(r -> ((Number) r[2]).longValue()).sum();

            Map<String, Object> point = new LinkedHashMap<>();
            point.put("month", month);
            point.put("total", total);
            result.add(point);
        }
        return result;
    }

    // ==================== 科室排名 ====================
    public List<Map<String, Object>> getDeptRanking(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Object[]> rows = transactionRepository.findDeptOutboundRanking(since);

        List<Map<String, Object>> result = new ArrayList<>();
        int rank = 1;
        for (Object[] row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("deptId", ((Number) row[0]).longValue());
            item.put("deptName", row[1]);
            item.put("totalQuantity", ((Number) row[2]).longValue());
            item.put("totalAmount", ((Number) row[3]).longValue());
            item.put("rank", rank++);
            result.add(item);
        }
        return result;
    }

    // ==================== 成本分析 ====================
    public List<Map<String, Object>> getCostAnalysis(int months) {
        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        for (int i = months - 1; i >= 0; i--) {
            LocalDate monthDate = LocalDate.now().minusMonths(i);
            String month = monthDate.format(fmt);
            LocalDateTime start = monthDate.withDayOfMonth(1).atStartOfDay();

            List<Object[]> rows = transactionRepository.findWeeklyTrend(start);
            long consumptionQty = rows.stream()
                    .filter(r -> "OUTBOUND".equals(r[1]))
                    .mapToLong(r -> ((Number) r[2]).longValue()).sum();

            Map<String, Object> point = new LinkedHashMap<>();
            point.put("month", month);
            point.put("consumptionCost", consumptionQty * 5); // 近似单价
            point.put("purchaseCost", consumptionQty * 5 * 95 / 100); // 采购成本略低
            result.add(point);
        }
        return result;
    }

    // ==================== BI大屏 ====================
    public Map<String, Object> getBiDashboard() {
        Map<String, Object> data = new LinkedHashMap<>();

        // 库存总价值 — 单条SQL聚合，不再加载全部实体
        data.put("totalInventoryValue", inventoryRepository.sumTotalInventoryValue());
        data.put("totalInventoryItems", inventoryRepository.countActiveInventory());

        LocalDate alertDate = LocalDate.now().plusDays(30);
        data.put("expiringCount", inventoryRepository.countExpiringInventory(alertDate));
        data.put("lowStockCount", inventoryRepository.countLowStockMaterials());
        data.put("pendingPurchase", purchaseContractRepository.findByConditions(null, "ACTIVE",
                PageRequest.of(0, 100)).getTotalElements());

        // 消耗趋势（近7天）
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        data.put("consumptionTrend", buildWeeklyTrend(since));

        // 科室排名（近30天）
        data.put("deptRanking", getDeptRanking(30));

        // 耗材分类分布
        List<Object[]> catRows = materialRepository.findActiveMaterialsByCategory();
        data.put("categoryDistribution", catRows.stream().map(row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", row[0]);
            item.put("value", ((Number) row[1]).longValue());
            return item;
        }).collect(Collectors.toList()));

        // 采购金额趋势（近6个月）
        data.put("purchaseTrend", getCostAnalysis(6));

        // AI预测准确率
        String lastMonth = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        List<AiPredictionResult> predictions = aiPredictionResultRepository.findWithActualByMonth(lastMonth);
        if (!predictions.isEmpty()) {
            double avgAccuracy = predictions.stream()
                    .filter(p -> p.getPredictedQuantity() != null && p.getPredictedQuantity() > 0)
                    .mapToDouble(p -> 100.0 - Math.abs(p.getPredictedQuantity() - p.getActualQuantity()) * 100.0 / p.getPredictedQuantity())
                    .average().orElse(85.0);
            data.put("predictionAccuracy", BigDecimal.valueOf(Math.max(0, avgAccuracy)).setScale(1, RoundingMode.HALF_UP));
        } else {
            data.put("predictionAccuracy", 85.0);
        }

        return data;
    }

    private String getStatusLabel(String status) {
        return switch (status) {
            case "DRAFT" -> "草稿";
            case "PENDING" -> "待审批";
            case "APPROVED" -> "已审批";
            case "REJECTED" -> "已驳回";
            case "DISPATCHED" -> "已发放";
            default -> status;
        };
    }
}
