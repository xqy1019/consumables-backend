package com.medical.system.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.system.entity.Inventory;
import com.medical.system.entity.InventoryTransaction;
import com.medical.system.repository.InventoryRepository;
import com.medical.system.repository.InventoryTransactionRepository;
import com.medical.system.repository.MaterialRepository;
import com.medical.system.repository.RequisitionRepository;
import com.medical.system.service.impl.AiPredictionServiceImpl.SafetyStockVO;
import com.medical.system.service.impl.AiPredictionServiceImpl.WarningVO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiDashboardService {

    private final ClaudeService claudeService;
    private final AiPredictionServiceImpl aiPredictionService;
    private final MaterialRepository materialRepo;
    private final InventoryRepository inventoryRepo;
    private final InventoryTransactionRepository transactionRepo;
    private final RequisitionRepository requisitionRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 5 分钟缓存
    private final AtomicReference<AiDashboardVO> cache = new AtomicReference<>();
    private final AtomicLong cacheTime = new AtomicLong(0);
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;

    public AiDashboardVO getDashboardAnalysis() {
        try {
            long now = System.currentTimeMillis();
            if (now - cacheTime.get() < CACHE_TTL_MS && cache.get() != null) {
                return cache.get();
            }

            // Claude 未配置，返回 null（前端走规则兜底）
            if (!claudeService.isConfigured()) return null;

            AiDashboardVO result = callClaude();
            if (result != null) {
                cache.set(result);
                cacheTime.set(now);
            }
            return result;
        } catch (Exception e) {
            log.warn("AI服务暂时不可用: {}", e.getMessage());
            return null;
        }
    }

    private AiDashboardVO callClaude() {
        try {
            // 收集原始数据
            List<WarningVO> rawWarnings = aiPredictionService.getWarnings();
            List<SafetyStockVO> rawSafety = aiPredictionService.getSafetyStock();

            long totalMaterials = materialRepo.count();
            long totalInventory = inventoryRepo.count();
            long pendingReqs = requisitionRepo.countByStatus("PENDING");

            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            List<InventoryTransaction> recentTx = transactionRepo.findAll().stream()
                    .filter(t -> t.getCreateTime() != null && t.getCreateTime().isAfter(sevenDaysAgo))
                    .collect(Collectors.toList());
            int inbound = recentTx.stream().filter(t -> "INBOUND".equals(t.getTransactionType()))
                    .mapToInt(InventoryTransaction::getQuantity).sum();
            int outbound = recentTx.stream().filter(t -> "OUTBOUND".equals(t.getTransactionType()))
                    .mapToInt(InventoryTransaction::getQuantity).sum();

            long expiringCount = inventoryRepo.findAll().stream()
                    .filter(inv -> inv.getStatus() == 1 && inv.getExpiryDate() != null
                            && inv.getExpiryDate().isBefore(LocalDate.now().plusDays(30)))
                    .count();

            // 构建 Prompt
            String prompt = buildPrompt(rawWarnings, rawSafety, totalMaterials, totalInventory,
                    pendingReqs, inbound, outbound, expiringCount);

            String raw = claudeService.chat(prompt);
            if (raw == null) return null;

            // 提取 JSON（Claude 偶尔会包一层 markdown 代码块）
            AiDashboardVO parsed = claudeService.extractJsonObject(raw,
                new com.fasterxml.jackson.core.type.TypeReference<AiDashboardVO>() {});
            if (parsed != null) return parsed;

            // 兜底：使用原有的字符串提取方式
            String json = extractJson(raw);
            return objectMapper.readValue(json, AiDashboardVO.class);

        } catch (Exception e) {
            log.error("AiDashboardService 解析失败：{}", e.getMessage());
            return null;
        }
    }

    private String buildPrompt(List<WarningVO> warnings, List<SafetyStockVO> safety,
                                long totalMaterials, long totalInventory, long pendingReqs,
                                int inbound, int outbound, long expiringCount) {

        // 格式化预警列表
        String warningText = warnings.isEmpty() ? "无预警" :
                warnings.stream().map(w -> String.format("[%s] %s - %s",
                        w.getSeverity(), w.getMaterialName(), w.getMessage()))
                        .collect(Collectors.joining("\n"));

        // 格式化低库存列表
        String safetyText = safety.isEmpty() ? "无低库存" :
                safety.stream().map(s -> String.format("%s：当前%d，缺口%d，建议补%d",
                        s.getMaterialName(), s.getCurrentStock(), s.getShortage(), s.getSuggestedPurchase()))
                        .collect(Collectors.joining("\n"));

        // 原始预警数据 JSON（让 Claude 直接引用字段值）
        String warningsJson = warnings.stream().map(w -> String.format(
                "{\"materialName\":\"%s\",\"severity\":\"%s\",\"type\":\"%s\"," +
                "\"message\":\"%s\",\"currentValue\":%d,\"threshold\":%d,\"daysLeft\":%s}",
                w.getMaterialName(), w.getSeverity(), w.getType(),
                w.getMessage().replace("\"", "'"),
                w.getCurrentValue() != null ? w.getCurrentValue() : 0,
                w.getThreshold() != null ? w.getThreshold() : 0,
                w.getDaysLeft() != null ? w.getDaysLeft().toString() : "null"
        )).collect(Collectors.joining(",", "[", "]"));

        String safetyJson = safety.stream().map(s -> String.format(
                "{\"materialName\":\"%s\",\"currentStock\":%d,\"shortage\":%d,\"suggestedPurchase\":%d}",
                s.getMaterialName(), s.getCurrentStock(), s.getShortage(), s.getSuggestedPurchase()
        )).collect(Collectors.joining(",", "[", "]"));

        return String.format("""
                你是医疗耗材管理系统的AI分析引擎。
                请根据以下真实库存快照，生成一份JSON格式的智能分析报告。

                要求：
                1. 只输出JSON，不要任何解释或markdown代码块
                2. aiReason字段限15字以内，精简直接
                3. healthScore根据预警严重程度、库存缺口综合评估（满分100）
                4. 严格按照下方JSON格式输出，字段名完全一致

                ===库存快照（%s）===
                耗材品种：%d种 | 库存批次：%d批 | 待审批申领：%d单
                近7天：入库%d件，出库%d件 | 近效期（30天内）：%d种

                预警列表：
                %s

                低库存列表：
                %s

                ===原始预警数据===
                %s

                ===原始补货数据===
                %s

                ===请严格返回以下格式的JSON===
                {
                  "insight": "一句话核心洞察，不超过50字",
                  "healthScore": 0到100的整数,
                  "healthLabel": "优秀或良好或需关注或危险",
                  "warnings": [
                    {
                      "materialName": "耗材名称",
                      "severity": "HIGH或MEDIUM或LOW",
                      "type": "EXPIRY或LOW_STOCK",
                      "message": "问题描述",
                      "aiReason": "AI分析（15字内）",
                      "currentValue": 数字,
                      "threshold": 数字,
                      "daysLeft": 数字或null
                    }
                  ],
                  "suggestions": [
                    {
                      "materialName": "耗材名称",
                      "currentStock": 数字,
                      "shortage": 数字,
                      "suggestedPurchase": 数字,
                      "priority": "紧急或重要或一般",
                      "aiReason": "AI建议（15字内）"
                    }
                  ],
                  "moduleStatus": [
                    {"module": "库存风控", "status": "ok或warn或error", "desc": "10字内"},
                    {"module": "需求预测", "status": "ok或warn或error", "desc": "10字内"},
                    {"module": "自动补货", "status": "ok或warn或error", "desc": "10字内"},
                    {"module": "到期监控", "status": "ok或warn或error", "desc": "10字内"}
                  ]
                }
                """,
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                totalMaterials, totalInventory, pendingReqs,
                inbound, outbound, expiringCount,
                warningText, safetyText,
                warningsJson, safetyJson
        );
    }

    /** 从 Claude 返回中提取纯 JSON（处理偶发的 markdown 包装、前缀说明文字等） */
    private String extractJson(String raw) {
        String s = raw.strip();
        // 1. 去除 ```json ... ``` 或 ``` ... ``` 包装
        if (s.contains("```")) {
            int start = s.indexOf("```");
            int codeStart = s.indexOf('\n', start);
            int end = s.lastIndexOf("```");
            if (codeStart > 0 && end > codeStart) {
                s = s.substring(codeStart + 1, end).strip();
            }
        }
        // 2. 找到第一个 { 到最后一个 } 之间的内容（兜底提取）
        int first = s.indexOf('{');
        int last  = s.lastIndexOf('}');
        if (first >= 0 && last > first) {
            s = s.substring(first, last + 1);
        }
        return s;
    }

    // DTO 定义
    @Data public static class AiDashboardVO {
        private String insight;
        private Integer healthScore;
        private String healthLabel;
        private List<AiWarningVO> warnings;
        private List<AiSuggestionVO> suggestions;
        private List<ModuleStatusVO> moduleStatus;
    }

    @Data public static class AiWarningVO {
        private String materialName;
        private String severity;
        private String type;
        private String message;
        private String aiReason;
        private Integer currentValue;
        private Integer threshold;
        private Integer daysLeft;
    }

    @Data public static class AiSuggestionVO {
        private String materialName;
        private Integer currentStock;
        private Integer shortage;
        private Integer suggestedPurchase;
        private String priority;
        private String aiReason;
    }

    @Data public static class ModuleStatusVO {
        private String module;
        private String status;
        private String desc;
    }
}
