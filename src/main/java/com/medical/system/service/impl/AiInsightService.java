package com.medical.system.service.impl;

import com.medical.system.entity.Inventory;
import com.medical.system.entity.Material;
import com.medical.system.entity.InventoryTransaction;
import com.medical.system.repository.InventoryRepository;
import com.medical.system.repository.InventoryTransactionRepository;
import com.medical.system.repository.MaterialRepository;
import com.medical.system.repository.RequisitionRepository;
import com.medical.system.service.impl.AiPredictionServiceImpl.WarningVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiInsightService {

    private final ClaudeService claudeService;
    private final AiPredictionServiceImpl aiPredictionService;
    private final MaterialRepository materialRepo;
    private final InventoryRepository inventoryRepo;
    private final InventoryTransactionRepository transactionRepo;
    private final RequisitionRepository requisitionRepo;

    // 简易 5 分钟内存缓存（无需引入 Redis/Caffeine）
    private final AtomicReference<String> cachedInsight = new AtomicReference<>();
    private final AtomicLong cacheTime = new AtomicLong(0);
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;

    public String getInsight() {
        try {
            // 命中缓存
            long now = System.currentTimeMillis();
            if (now - cacheTime.get() < CACHE_TTL_MS && cachedInsight.get() != null) {
                return cachedInsight.get();
            }

            // Claude 未配置时返回 null（前端走兜底文案）
            if (!claudeService.isConfigured()) {
                return null;
            }

            String insight = callClaude();
            if (insight != null) {
                cachedInsight.set(insight);
                cacheTime.set(now);
            }
            return insight;
        } catch (Exception e) {
            log.warn("AI服务暂时不可用: {}", e.getMessage());
            return null;
        }
    }

    private String callClaude() {
        try {
            String prompt = buildPrompt();
            log.debug("发送 Claude Insight 请求，Prompt 长度：{}", prompt.length());
            String result = claudeService.chat(prompt);
            log.debug("Claude 返回：{}", result);
            return result;
        } catch (Exception e) {
            log.error("AiInsightService 调用失败：{}", e.getMessage());
            return null;
        }
    }

    private String buildPrompt() {
        LocalDate today = LocalDate.now();

        // —— 耗材 & 库存基础数据 ——
        long totalMaterials = materialRepo.count();
        long totalInventory = inventoryRepo.count();

        // —— 预警 ——
        List<WarningVO> warnings = aiPredictionService.getWarnings();
        long highCount   = warnings.stream().filter(w -> "HIGH".equals(w.getSeverity())).count();
        long mediumCount = warnings.stream().filter(w -> "MEDIUM".equals(w.getSeverity())).count();
        long lowCount    = warnings.stream().filter(w -> "LOW".equals(w.getSeverity())).count();

        // Top 3 高危预警
        String topWarnings = warnings.stream()
                .filter(w -> "HIGH".equals(w.getSeverity()))
                .limit(3)
                .map(w -> String.format("  · %s：%s", w.getMaterialName(), w.getMessage()))
                .collect(Collectors.joining("\n"));
        if (topWarnings.isBlank()) topWarnings = "  · 无高危预警";

        // —— 低库存 ——
        List<AiPredictionServiceImpl.SafetyStockVO> safetyList = aiPredictionService.getSafetyStock();
        long shortageCount = safetyList.size();
        String topShortage = safetyList.stream()
                .sorted(Comparator.comparingInt(AiPredictionServiceImpl.SafetyStockVO::getShortage).reversed())
                .limit(3)
                .map(s -> String.format("  · %s：当前库存 %d，缺口 %d", s.getMaterialName(), s.getCurrentStock(), s.getShortage()))
                .collect(Collectors.joining("\n"));
        if (topShortage.isBlank()) topShortage = "  · 无低库存耗材";

        // —— 申领单 ——
        long pendingRequisitions = requisitionRepo.countByStatus("PENDING");

        // —— 近 7 天出入库 ——
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<InventoryTransaction> recentTx = transactionRepo.findAll().stream()
                .filter(t -> t.getCreateTime() != null && t.getCreateTime().isAfter(sevenDaysAgo))
                .collect(Collectors.toList());
        int inbound  = recentTx.stream().filter(t -> "INBOUND".equals(t.getTransactionType()))
                .mapToInt(InventoryTransaction::getQuantity).sum();
        int outbound = recentTx.stream().filter(t -> "OUTBOUND".equals(t.getTransactionType()))
                .mapToInt(InventoryTransaction::getQuantity).sum();

        // —— 近效期（30天内）——
        List<Inventory> expiring = inventoryRepo.findAll().stream()
                .filter(inv -> inv.getStatus() == 1 && inv.getExpiryDate() != null
                        && inv.getExpiryDate().isBefore(today.plusDays(30)))
                .collect(Collectors.toList());

        return String.format("""
                你是一个医疗耗材管理系统的 AI 运营助手。
                请根据下方实时数据，用【一句话】给出最关键的运营洞察（50字以内，简洁直接，不要客套，不要分析过程，只给结论）。

                【当前时间】%s
                【耗材总品种】%d 种
                【库存批次】%d 批
                【待审批申领】%d 单
                【预警汇总】高危 %d 条 · 中危 %d 条 · 低危 %d 条
                【高危预警明细】
                %s
                【低库存品种数】%d 种，缺口最大的：
                %s
                【近效期品种数】%d 种（30天内到期）
                【近7天流水】入库 %d 件，出库 %d 件

                请直接输出洞察结论，不要加任何前缀或解释。
                """,
                today.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")),
                totalMaterials, totalInventory, pendingRequisitions,
                highCount, mediumCount, lowCount,
                topWarnings,
                shortageCount, topShortage,
                expiring.size(),
                inbound, outbound
        );
    }
}
