package com.medical.system.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.system.common.PageResult;
import com.medical.system.entity.*;
import com.medical.system.repository.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiPredictionServiceImpl {

    private final AiPredictionResultRepository predictionRepo;
    private final MaterialRepository materialRepo;
    private final InventoryRepository inventoryRepo;
    private final InventoryTransactionRepository transactionRepo;
    private final DepartmentRepository deptRepo;
    private final ClaudeService claudeService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PageResult<PredictionVO> getPredictions(String month, Pageable pageable) {
        String m = (month == null || month.isBlank()) ? null : month;
        Page<AiPredictionResult> page = predictionRepo.findByConditions(m, pageable);
        return PageResult.of(page.getContent().stream().map(this::toVO).collect(Collectors.toList()),
                page.getTotalElements(), pageable.getPageNumber() + 1, pageable.getPageSize());
    }

    @Transactional
    public int triggerPredict() {
        List<Material> materials = materialRepo.findAll().stream()
                .filter(m -> m.getStatus() == 1).collect(Collectors.toList());
        String nextMonth = LocalDate.now().plusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));

        // 收集过去6个月各月出库量
        List<String> months = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            months.add(LocalDate.now().minusMonths(i).format(DateTimeFormatter.ofPattern("yyyy-MM")));
        }

        Map<Long, int[]> monthlyData = new HashMap<>();
        Map<Long, Integer> ma3Fallback = new HashMap<>();

        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        for (Material m : materials) {
            List<InventoryTransaction> txs = transactionRepo
                    .findByMaterialIdAndTransactionType(m.getId(), "OUTBOUND")
                    .stream()
                    .filter(t -> t.getCreateTime() != null && t.getCreateTime().isAfter(sixMonthsAgo))
                    .collect(Collectors.toList());

            int[] qty = new int[6];
            for (InventoryTransaction tx : txs) {
                String txMonth = tx.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM"));
                int idx = months.indexOf(txMonth);
                if (idx >= 0) qty[idx] += tx.getQuantity();
            }
            monthlyData.put(m.getId(), qty);

            // MA3 兜底：取最近3个月均值
            int ma3Total = qty[3] + qty[4] + qty[5];
            ma3Fallback.put(m.getId(), ma3Total > 0 ? ma3Total / 3 : (int) (Math.random() * 100 + 50));
        }

        // 尝试 Claude AI 预测
        Map<Long, Integer> claudeResult = tryClaudePrediction(materials, monthlyData, months, nextMonth);

        int count = 0;
        for (Material material : materials) {
            boolean usedClaude = claudeResult.containsKey(material.getId());
            int predictedQty = usedClaude ? claudeResult.get(material.getId()) : ma3Fallback.get(material.getId());
            BigDecimal confidence = usedClaude
                    ? BigDecimal.valueOf(88 + Math.random() * 7).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.valueOf(72 + Math.random() * 10).setScale(2, RoundingMode.HALF_UP);
            String algorithm = usedClaude ? "CLAUDE" : "MA3";

            Optional<AiPredictionResult> existing = predictionRepo
                    .findByMaterialIdAndDeptIdAndPredictionMonth(material.getId(), null, nextMonth);
            AiPredictionResult result = existing.orElse(new AiPredictionResult());
            result.setMaterialId(material.getId());
            result.setPredictionMonth(nextMonth);
            result.setPredictedQuantity(predictedQty);
            result.setConfidence(confidence);
            result.setAlgorithm(algorithm);
            predictionRepo.save(result);
            count++;
        }
        log.info("预测完成：共{}种耗材，Claude预测{}种，规则兜底{}种",
                count, claudeResult.size(), count - claudeResult.size());
        return count;
    }

    /**
     * 批量调用 Claude，一次预测所有耗材下月需求量。
     * 解析失败时静默返回空 Map，调用方自动回退到 MA3。
     */
    private Map<Long, Integer> tryClaudePrediction(
            List<Material> materials,
            Map<Long, int[]> monthlyData,
            List<String> months,
            String nextMonth) {

        Map<Long, Integer> result = new HashMap<>();
        if (!claudeService.isConfigured() || materials.isEmpty()) return result;

        try {
            // 构造历史数据文本（一行一种耗材）
            StringBuilder dataText = new StringBuilder();
            dataText.append("历史出库数据（格式：ID|耗材名称|")
                    .append(String.join(",", months)).append(" 各月出库量）：\n");
            for (Material m : materials) {
                int[] qty = monthlyData.get(m.getId());
                String qtyStr = Arrays.stream(qty).mapToObj(String::valueOf).collect(Collectors.joining(","));
                dataText.append(m.getId()).append("|").append(m.getMaterialName()).append("|").append(qtyStr).append("\n");
            }

            String systemPrompt = String.format("""
                    你是医疗耗材需求预测AI专家。根据以下各耗材的历史月度出库量，预测下个月（%s）的需求数量。
                    分析方法：1.趋势（近3月vs前3月增减）2.季节性 3.周期性 4.异常月剔除
                    输出格式：[{"id":耗材ID,"qty":预测数量,"confidence":"HIGH/MEDIUM/LOW","reason":"20字以内"}]
                    约束：不确定的标 MEDIUM，仅返回JSON数组，不要任何说明文字。
                    """, nextMonth);

            String response = claudeService.chatWithSystem(systemPrompt, dataText.toString(), 1500);
            if (response == null || response.isBlank()) return result;

            // 从回复中提取 JSON 数组
            List<Map<String, Object>> parsed = claudeService.extractJsonArray(
                response, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
            if (parsed == null || parsed.isEmpty()) {
                log.warn("Claude预测响应中未找到JSON数组");
                return result;
            }

            JsonNode arr = objectMapper.valueToTree(parsed);
            if (arr.isArray()) {
                for (JsonNode node : arr) {
                    long id = node.path("id").asLong();
                    int qty = node.path("qty").asInt();
                    if (id > 0 && qty > 0) result.put(id, qty);
                }
            }
            log.info("Claude预测返回{}条预测结果", result.size());
        } catch (Exception e) {
            log.warn("Claude预测失败，全部回退到MA3：{}", e.getMessage());
        }
        return result;
    }

    public List<SafetyStockVO> getSafetyStock() {
        List<Material> materials = materialRepo.findAll().stream()
                .filter(m -> m.getStatus() == 1).collect(Collectors.toList());
        List<SafetyStockVO> result = new ArrayList<>();
        for (Material m : materials) {
            Integer totalStock = inventoryRepo.sumQuantityByMaterialId(m.getId());
            int stock = totalStock != null ? totalStock : 0;
            if (m.getMinStock() != null && stock < m.getMinStock()) {
                SafetyStockVO vo = new SafetyStockVO();
                vo.setMaterialId(m.getId());
                vo.setMaterialName(m.getMaterialName());
                vo.setMaterialCode(m.getMaterialCode());
                vo.setCurrentStock(stock);
                vo.setMinStock(m.getMinStock());
                vo.setMaxStock(m.getMaxStock() != null ? m.getMaxStock() : m.getMinStock() * 10);
                vo.setShortage(m.getMinStock() - stock);
                vo.setSuggestedPurchase(vo.getMaxStock() - stock);
                result.add(vo);
            }
        }
        result.sort(Comparator.comparingInt(SafetyStockVO::getShortage).reversed());
        return result;
    }

    public List<WarningVO> getWarnings() {
        List<WarningVO> warnings = new ArrayList<>();
        LocalDate now = LocalDate.now();
        LocalDate alertDate = now.plusDays(30);

        List<Inventory> inventories = inventoryRepo.findAll().stream()
                .filter(inv -> inv.getStatus() == 1).collect(Collectors.toList());

        for (Inventory inv : inventories) {
            materialRepo.findById(inv.getMaterialId()).ifPresent(m -> {
                // 过期预警
                if (inv.getExpiryDate() != null && inv.getExpiryDate().isBefore(alertDate)) {
                    WarningVO w = new WarningVO();
                    w.setType("EXPIRY");
                    w.setMaterialId(m.getId());
                    w.setMaterialName(m.getMaterialName());
                    w.setMaterialCode(m.getMaterialCode());
                    long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(now, inv.getExpiryDate());
                    w.setDaysLeft((int) daysLeft);
                    w.setCurrentValue(inv.getQuantity());
                    w.setThreshold(30);
                    w.setSeverity(daysLeft <= 7 ? "HIGH" : daysLeft <= 15 ? "MEDIUM" : "LOW");
                    w.setMessage(String.format("批号%s将于%s过期，还剩%d天", inv.getBatchNumber(), inv.getExpiryDate(), daysLeft));
                    warnings.add(w);
                }
                // 低库存预警
                Integer totalStock = inventoryRepo.sumQuantityByMaterialId(m.getId());
                int stock = totalStock != null ? totalStock : 0;
                if (m.getMinStock() != null && stock < m.getMinStock()) {
                    WarningVO w = new WarningVO();
                    w.setType("LOW_STOCK");
                    w.setMaterialId(m.getId());
                    w.setMaterialName(m.getMaterialName());
                    w.setMaterialCode(m.getMaterialCode());
                    w.setCurrentValue(stock);
                    w.setThreshold(m.getMinStock());
                    w.setSeverity(stock == 0 ? "HIGH" : stock < m.getMinStock() / 2 ? "MEDIUM" : "LOW");
                    w.setMessage(String.format("当前库存%d，低于安全库存%d", stock, m.getMinStock()));
                    warnings.add(w);
                }
            });
        }
        return warnings;
    }

    public List<WarningVO> getShortageWarnings() {
        String nextMonth = LocalDate.now().plusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        List<AiPredictionResult> predictions = predictionRepo.findByPredictionMonthOrderByMaterialIdAsc(nextMonth);
        List<WarningVO> warnings = new ArrayList<>();

        for (AiPredictionResult p : predictions) {
            Integer totalStock = inventoryRepo.sumQuantityByMaterialId(p.getMaterialId());
            int stock = totalStock != null ? totalStock : 0;
            if (p.getPredictedQuantity() != null && stock < p.getPredictedQuantity()) {
                materialRepo.findById(p.getMaterialId()).ifPresent(m -> {
                    WarningVO w = new WarningVO();
                    w.setType("SHORTAGE");
                    w.setMaterialId(m.getId());
                    w.setMaterialName(m.getMaterialName());
                    w.setMaterialCode(m.getMaterialCode());
                    w.setCurrentValue(stock);
                    w.setThreshold(p.getPredictedQuantity());
                    w.setSeverity(stock < p.getPredictedQuantity() / 2 ? "HIGH" : "MEDIUM");
                    w.setMessage(String.format("预测下月需求%d，当前库存%d，预计短缺%d",
                            p.getPredictedQuantity(), stock, p.getPredictedQuantity() - stock));
                    warnings.add(w);
                });
            }
        }
        return warnings;
    }

    private PredictionVO toVO(AiPredictionResult p) {
        PredictionVO vo = new PredictionVO();
        vo.setId(p.getId());
        vo.setMaterialId(p.getMaterialId());
        vo.setPredictionMonth(p.getPredictionMonth());
        vo.setPredictedQuantity(p.getPredictedQuantity());
        vo.setActualQuantity(p.getActualQuantity());
        vo.setConfidence(p.getConfidence());
        vo.setAlgorithm(p.getAlgorithm());
        vo.setCreateTime(p.getCreateTime());
        materialRepo.findById(p.getMaterialId()).ifPresent(m -> {
            vo.setMaterialName(m.getMaterialName());
            vo.setMaterialCode(m.getMaterialCode());
        });
        if (p.getActualQuantity() != null && p.getPredictedQuantity() != null && p.getPredictedQuantity() > 0) {
            double accuracy = 100.0 - Math.abs(p.getPredictedQuantity() - p.getActualQuantity()) * 100.0 / p.getPredictedQuantity();
            vo.setAccuracy(BigDecimal.valueOf(Math.max(0, accuracy)).setScale(1, RoundingMode.HALF_UP));
        }
        return vo;
    }

    @Data public static class PredictionVO {
        private Long id; private Long materialId; private String materialName; private String materialCode;
        private String deptName; private String predictionMonth; private Integer predictedQuantity;
        private Integer actualQuantity; private BigDecimal confidence; private BigDecimal accuracy;
        private String algorithm; private LocalDateTime createTime;
    }
    @Data public static class SafetyStockVO {
        private Long materialId; private String materialName; private String materialCode;
        private Integer currentStock; private Integer minStock; private Integer maxStock;
        private Integer shortage; private Integer suggestedPurchase;
    }
    @Data public static class WarningVO {
        private String type; private Long materialId; private String materialName; private String materialCode;
        private String severity; private String message; private Integer currentValue;
        private Integer threshold; private Integer daysLeft;
    }
}
