package com.medical.system.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.system.dto.RequisitionRecommendationVO;
import com.medical.system.entity.AiPredictionResult;
import com.medical.system.entity.InventoryTransaction;
import com.medical.system.entity.Material;
import com.medical.system.repository.AiPredictionResultRepository;
import com.medical.system.repository.InventoryRepository;
import com.medical.system.repository.InventoryTransactionRepository;
import com.medical.system.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiRequisitionRecommendService {

    private final InventoryTransactionRepository transactionRepository;
    private final AiPredictionResultRepository predictionRepository;
    private final InventoryRepository inventoryRepository;
    private final MaterialRepository materialRepository;
    private final ClaudeService claudeService;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    public List<RequisitionRecommendationVO> recommend(Long deptId) {
        // 1. 查询近6月 OUTBOUND 记录
        LocalDateTime since = LocalDateTime.now().minusMonths(6);
        List<InventoryTransaction> transactions = transactionRepository.findOutboundByDeptSince(deptId, since);

        // 2. 按 materialId + month 聚合月消耗量
        Map<Long, Map<String, Integer>> materialMonthConsumption = new LinkedHashMap<>();
        for (InventoryTransaction t : transactions) {
            String month = t.getCreateTime().format(MONTH_FMT);
            materialMonthConsumption
                .computeIfAbsent(t.getMaterialId(), k -> new HashMap<>())
                .merge(month, t.getQuantity() != null ? t.getQuantity() : 0, Integer::sum);
        }

        if (materialMonthConsumption.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 查询下月预测数据
        String nextMonth = LocalDateTime.now().plusMonths(1).format(MONTH_FMT);
        List<AiPredictionResult> predictions = predictionRepository.findByPredictionMonthOrderByMaterialIdAsc(nextMonth);
        Map<Long, Integer> predictionMap = predictions.stream()
            .collect(Collectors.toMap(
                AiPredictionResult::getMaterialId,
                p -> p.getPredictedQuantity() != null ? p.getPredictedQuantity() : 0,
                Integer::sum));

        // 4. 查询活跃耗材信息
        List<Material> materials = materialRepository.findByStatus(1);
        Map<Long, Material> materialMap = materials.stream()
            .collect(Collectors.toMap(Material::getId, m -> m));

        // 5. 季节系数（基于下月月份）
        int nextMonthValue = LocalDateTime.now().plusMonths(1).getMonthValue();
        double seasonFactor = getSeasonFactor(nextMonthValue);

        // 6. 计算推荐数量
        List<RequisitionRecommendationVO> results = new ArrayList<>();
        for (Map.Entry<Long, Map<String, Integer>> entry : materialMonthConsumption.entrySet()) {
            Long materialId = entry.getKey();
            Material material = materialMap.get(materialId);
            if (material == null) continue;

            // MA3：最近3个月均值作为基础预测
            int ma3 = calcMA3(entry.getValue());

            // 优先使用 AI 预测值，否则使用 MA3
            int predictedConsumption = predictionMap.getOrDefault(materialId, ma3);

            // 应用季节系数
            int adjustedPrediction = (int) Math.round(predictedConsumption * seasonFactor);

            // 当前库存
            Integer currentStock = inventoryRepository.sumQuantityByMaterialId(materialId);
            if (currentStock == null) currentStock = 0;

            // 推荐数量 = MAX(0, 预测消耗×季节系数×1.1 - 当前库存)（含10%安全缓冲）
            int recommended = (int) Math.round(Math.max(0.0, adjustedPrediction * 1.1 - currentStock));
            if (recommended <= 0) continue;

            RequisitionRecommendationVO vo = new RequisitionRecommendationVO();
            vo.setMaterialId(materialId);
            vo.setMaterialName(material.getMaterialName());
            vo.setSpecification(material.getSpecification());
            vo.setUnit(material.getUnit());
            vo.setRecommendedQuantity(recommended);
            vo.setCurrentStock(currentStock);
            vo.setPredictedConsumption(adjustedPrediction);
            vo.setReason("基于历史消耗预测，建议补充库存");
            vo.setUrgency(calcUrgency(currentStock, adjustedPrediction));
            results.add(vo);
        }

        // 7. 取推荐数量前15种，按推荐数量降序
        results.sort(Comparator.comparingInt(RequisitionRecommendationVO::getRecommendedQuantity).reversed());
        List<RequisitionRecommendationVO> top15 = results.stream().limit(15).collect(Collectors.toList());

        // 8. 调用 Claude 丰富理由和紧迫程度
        enrichWithClaude(top15);

        return top15;
    }

    private int calcMA3(Map<String, Integer> monthlyConsumption) {
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(monthlyConsumption.entrySet());
        sorted.sort(Map.Entry.<String, Integer>comparingByKey().reversed());
        int sum = 0, count = 0;
        for (int i = 0; i < Math.min(3, sorted.size()); i++) {
            sum += sorted.get(i).getValue();
            count++;
        }
        return count > 0 ? sum / count : 0;
    }

    private double getSeasonFactor(int month) {
        if (month == 1 || month == 2 || month == 12) return 1.2;
        if (month == 6 || month == 7 || month == 8) return 1.1;
        return 1.0;
    }

    private String calcUrgency(int currentStock, int predictedConsumption) {
        if (predictedConsumption <= 0) return "LOW";
        double ratio = (double) currentStock / predictedConsumption;
        if (ratio < 0.3) return "HIGH";
        if (ratio < 0.7) return "MEDIUM";
        return "LOW";
    }

    private void enrichWithClaude(List<RequisitionRecommendationVO> items) {
        if (items.isEmpty() || !claudeService.isConfigured()) return;
        try {
            List<Map<String, Object>> data = items.stream().map(vo -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("materialId", vo.getMaterialId());
                m.put("materialName", vo.getMaterialName());
                m.put("currentStock", vo.getCurrentStock());
                m.put("predictedQty", vo.getPredictedConsumption());
                m.put("recommendedQty", vo.getRecommendedQuantity());
                return m;
            }).collect(Collectors.toList());

            String systemPrompt = """
                你是医院耗材管理专家，根据各耗材的历史消耗、预测数量和当前库存，\
                给出简洁的补货建议理由（每条不超过30字）和紧迫程度（HIGH/MEDIUM/LOW）。\
                返回纯 JSON 数组，格式：[{"materialId":1,"reason":"...","urgency":"HIGH"}]，\
                不要有任何额外说明，只返回JSON数组。
                """;
            String userMsg = objectMapper.writeValueAsString(data);
            String result = claudeService.chatWithSystem(systemPrompt, userMsg, 1000);
            if (result == null || result.isBlank()) return;

            List<Map<String, Object>> claudeResults = claudeService.extractJsonArray(
                result, new TypeReference<List<Map<String, Object>>>() {});
            if (claudeResults == null) return;

            Map<Long, Map<String, Object>> claudeMap = claudeResults.stream()
                .filter(m -> m.containsKey("materialId"))
                .collect(Collectors.toMap(
                    m -> Long.valueOf(m.get("materialId").toString()),
                    m -> m,
                    (a, b) -> a));

            for (RequisitionRecommendationVO vo : items) {
                Map<String, Object> claudeData = claudeMap.get(vo.getMaterialId());
                if (claudeData == null) continue;
                if (claudeData.get("reason") != null) {
                    vo.setReason(claudeData.get("reason").toString());
                }
                if (claudeData.get("urgency") != null) {
                    String urgency = claudeData.get("urgency").toString().toUpperCase();
                    if (List.of("HIGH", "MEDIUM", "LOW").contains(urgency)) {
                        vo.setUrgency(urgency);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Claude enrichment 失败，使用默认理由: {}", e.getMessage());
        }
    }
}
