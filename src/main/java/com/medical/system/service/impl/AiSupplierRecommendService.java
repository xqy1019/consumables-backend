package com.medical.system.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.system.repository.InventoryRepository;
import com.medical.system.repository.PurchaseContractItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiSupplierRecommendService {

    private final PurchaseContractItemRepository contractItemRepository;
    private final InventoryRepository inventoryRepository;
    private final ClaudeService claudeService;
    private final ObjectMapper objectMapper;

    public List<Map<String, Object>> recommend(Long materialId, Integer quantity) {
        // 1. 查有该耗材历史供货记录的供应商
        List<Object[]> supplierStats = contractItemRepository.findSupplierPriceStats(materialId);

        if (supplierStats == null || supplierStats.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 提取供应商ID列表
        List<Long> supplierIds = new ArrayList<>();
        for (Object[] stat : supplierStats) {
            if (stat[0] != null) {
                supplierIds.add(((Number) stat[0]).longValue());
            }
        }

        // 3. 查质量合格率
        List<Object[]> qualityStats = inventoryRepository.findQualityRateBySupplierIds(supplierIds);
        Map<Long, Double> qualityRateMap = new HashMap<>();
        for (Object[] q : qualityStats) {
            if (q[0] != null && q[1] != null && q[2] != null) {
                long supplierId = ((Number) q[0]).longValue();
                long total = ((Number) q[1]).longValue();
                long passed = ((Number) q[2]).longValue();
                qualityRateMap.put(supplierId, total > 0 ? passed * 100.0 / total : 0.0);
            }
        }

        // 4. 构建供应商数据
        List<Map<String, Object>> supplierData = new ArrayList<>();
        for (Object[] stat : supplierStats) {
            Map<String, Object> data = new HashMap<>();
            long supplierId = ((Number) stat[0]).longValue();
            data.put("supplierId", supplierId);
            data.put("avgPrice", stat[1]);
            data.put("orderCount", stat[2]);
            data.put("qualityRate", qualityRateMap.getOrDefault(supplierId, 0.0));
            supplierData.add(data);
        }

        // 5. 调 Claude 打分排序
        if (!claudeService.isConfigured()) {
            return supplierData.subList(0, Math.min(5, supplierData.size()));
        }

        try {
            String prompt = String.format(
                    "根据以下供应商数据，为耗材ID=%d(需求量=%d)推荐最优供应商。" +
                    "数据：%s。" +
                    "请综合价格（权重40%%）、质量合格率（权重40%%）、历史订单量（权重20%%）进行评分。" +
                    "仅返回JSON数组：[{\"supplierId\":ID,\"score\":0-100整数,\"reason\":\"推荐理由20字以内\"}]",
                    materialId, quantity, objectMapper.writeValueAsString(supplierData)
            );

            String response = claudeService.chat(prompt);
            if (response == null) {
                return supplierData.subList(0, Math.min(5, supplierData.size()));
            }

            // 提取 JSON 数组（使用健壮的正则提取，避免嵌套/特殊字符失败）
            List<Map<String, Object>> scored = claudeService.extractJsonArray(
                    response, new TypeReference<List<Map<String, Object>>>() {});
            if (scored == null) {
                return supplierData.subList(0, Math.min(5, supplierData.size()));
            }

            // 合并评分和原始数据
            for (Map<String, Object> sc : scored) {
                Long sid = ((Number) sc.get("supplierId")).longValue();
                for (Map<String, Object> d : supplierData) {
                    if (sid.equals(((Number) d.get("supplierId")).longValue())) {
                        d.put("score", sc.get("score"));
                        d.put("reason", sc.get("reason"));
                        break;
                    }
                }
            }

            // 排序取Top5
            supplierData.sort((a, b) -> {
                int scoreA = a.get("score") instanceof Number ? ((Number) a.get("score")).intValue() : 0;
                int scoreB = b.get("score") instanceof Number ? ((Number) b.get("score")).intValue() : 0;
                return Integer.compare(scoreB, scoreA);
            });
        } catch (Exception ex) {
            log.warn("供应商推荐 Claude 调用失败: {}", ex.getMessage());
        }

        return supplierData.subList(0, Math.min(5, supplierData.size()));
    }
}
