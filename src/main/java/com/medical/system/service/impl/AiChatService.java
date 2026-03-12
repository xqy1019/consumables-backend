package com.medical.system.service.impl;

import com.medical.system.repository.InventoryRepository;
import com.medical.system.repository.MaterialRepository;
import com.medical.system.repository.RequisitionRepository;
import com.medical.system.service.impl.AiPredictionServiceImpl.SafetyStockVO;
import com.medical.system.service.impl.AiPredictionServiceImpl.WarningVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final ClaudeService claudeService;
    private final AiPredictionServiceImpl aiPredictionService;
    private final MaterialRepository materialRepo;
    private final InventoryRepository inventoryRepo;
    private final RequisitionRepository requisitionRepo;

    private static final int CHAT_MAX_TOKENS = 600;

    public ChatResponseVO chat(String userMessage) {
        // 意图识别（关键词规则，不依赖 Claude，始终执行）
        ActionVO action = detectAction(userMessage);

        String reply;
        try {
            if (claudeService.isConfigured()) {
                String systemPrompt = buildSystemPrompt();
                reply = claudeService.chatWithSystem(systemPrompt, userMessage, CHAT_MAX_TOKENS);
                if (reply == null) reply = ruleFallback(userMessage);
            } else {
                reply = ruleFallback(userMessage);
            }
        } catch (Exception e) {
            log.warn("AI服务暂时不可用: {}", e.getMessage());
            reply = ruleFallback(userMessage);
        }

        return new ChatResponseVO(reply, action);
    }

    // ── 意图识别 ─────────────────────────────────────────────────────────────

    private ActionVO detectAction(String msg) {
        if (anyMatch(msg, "临期", "过期", "即将到期", "处置")) {
            return new ActionVO("navigate", "/ai/warnings?tab=expiry");
        }
        if (anyMatch(msg, "消耗异常", "异常消耗", "用量异常", "异常用量")) {
            return new ActionVO("navigate", "/ai/warnings?tab=anomaly");
        }
        if (anyMatch(msg, "预警", "警告", "高危", "告警")) {
            return new ActionVO("navigate", "/ai/warnings");
        }
        if (anyMatch(msg, "需求预测", "用量预测") || (anyMatch(msg, "预测") && anyMatch(msg, "用量", "月份"))) {
            return new ActionVO("navigate", "/ai/prediction");
        }
        if (anyMatch(msg, "补货", "库存不足", "缺口") || (anyMatch(msg, "采购") && anyMatch(msg, "建议", "帮我", "怎么"))) {
            return new ActionVO("open_purchase", "/purchase/requisition");
        }
        if (anyMatch(msg, "申领", "审批", "申请单")) {
            return new ActionVO("navigate", "/requisitions");
        }
        if (anyMatch(msg, "采购单", "请购单")) {
            return new ActionVO("navigate", "/purchase/requisition");
        }
        if (anyMatch(msg, "库存") && anyMatch(msg, "查看", "管理", "详情", "列表")) {
            return new ActionVO("navigate", "/inventory");
        }
        // 询问具体耗材库存时，跳转到库存页并高亮筛选
        if (anyMatch(msg, "库存", "还有多少", "剩余", "剩多少")) {
            String keyword = msg.replaceAll("还有多少|库存|剩余|剩多少|查询|查一下|帮我查|告诉我", "").trim();
            if (keyword.length() >= 2) {
                return new ActionVO("navigate", "/inventory?keyword=" + keyword);
            }
        }
        return null;
    }

    private boolean anyMatch(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    // ── 系统提示词（注入实时数据给 Claude） ──────────────────────────────────

    private String buildSystemPrompt() {
        try {
            List<WarningVO>     warnings = aiPredictionService.getWarnings();
            List<SafetyStockVO> safety   = aiPredictionService.getSafetyStock();
            long highCount = warnings.stream().filter(w -> "HIGH".equals(w.getSeverity())).count();
            long medCount  = warnings.stream().filter(w -> "MEDIUM".equals(w.getSeverity())).count();

            String warningDetail = warnings.stream()
                    .filter(w -> "HIGH".equals(w.getSeverity())).limit(5)
                    .map(w -> "  - " + w.getMaterialName() + "：" + w.getMessage())
                    .collect(Collectors.joining("\n"));
            if (warningDetail.isBlank()) warningDetail = "  - 无高危预警";

            String shortageDetail = safety.stream()
                    .filter(s -> s.getShortage() > 0).limit(5)
                    .map(s -> String.format("  - %s：当前库存 %d，缺口 %d，建议补货 %d",
                            s.getMaterialName(), s.getCurrentStock(), s.getShortage(), s.getSuggestedPurchase()))
                    .collect(Collectors.joining("\n"));
            if (shortageDetail.isBlank()) shortageDetail = "  - 无低库存耗材";

            long lowStockCount = safety.stream().filter(s -> s.getShortage() > 0).count();

            // 全量耗材库存快照（供回答具体耗材库存问题）
            String inventorySnapshot = safety.stream()
                    .map(s -> String.format("  - %s：%d件%s",
                            s.getMaterialName(),
                            s.getCurrentStock(),
                            s.getShortage() > 0 ? "（⚠低库存，缺口" + s.getShortage() + "）" : ""))
                    .collect(Collectors.joining("\n"));
            if (inventorySnapshot.isBlank()) inventorySnapshot = "  - 暂无库存数据";

            return String.format("""
                    角色：医院耗材管理智能助手，具备医疗器械和临床知识。

                    当前系统状态：
                    - 高危预警 %d 条，中危 %d 条
                    - 低库存品种 %d 种

                    高危预警明细：
                    %s

                    低库存明细（需补货）：
                    %s

                    全量耗材库存快照（回答具体耗材库存问题时使用）：
                    %s

                    回答规则：
                    1. 只回答耗材管理相关问题，其他礼貌拒绝
                    2. 回答控制在 200 字以内，可用 Markdown 列表
                    3. 回答具体耗材库存时，必须从"全量耗材库存快照"中查找对应耗材，直接给出数量
                    4. 如果用户问的耗材在快照中找不到，说"系统中暂无该耗材记录"
                    5. 无法确定时，说明假设条件后再回答
                    6. 不提供具体医疗诊断建议
                    """,
                    highCount, medCount,
                    lowStockCount,
                    warningDetail, shortageDetail, inventorySnapshot
            );
        } catch (Exception e) {
            log.error("构建 Chat 系统提示词失败：{}", e.getMessage());
            return "你是一个医院使用的 AI 助手，既负责医疗耗材管理系统的问题，也能回答临床医学知识。请用中文简洁回答，回答不超过 200 字，医学建议注明遵医嘱。";
        }
    }

    // ── 规则兜底（Claude 未配置时） ───────────────────────────────────────────

    private String ruleFallback(String msg) {
        try {
            List<WarningVO>     warnings = aiPredictionService.getWarnings();
            List<SafetyStockVO> safety   = aiPredictionService.getSafetyStock();
            long pending = requisitionRepo.countByStatus("PENDING");

            if (anyMatch(msg, "预警", "警告", "高危", "告警")) {
                long high = warnings.stream().filter(w -> "HIGH".equals(w.getSeverity())).count();
                long med  = warnings.stream().filter(w -> "MEDIUM".equals(w.getSeverity())).count();
                if (warnings.isEmpty()) return "当前系统无预警，库存状态良好。";
                String top = warnings.stream().filter(w -> "HIGH".equals(w.getSeverity())).limit(2)
                        .map(w -> w.getMaterialName() + "：" + w.getMessage())
                        .collect(Collectors.joining("；"));
                return String.format("当前共有 %d 条预警（高危 %d 条、中危 %d 条）。高危详情：%s", warnings.size(), high, med, top);
            }
            if (anyMatch(msg, "补货", "库存不足", "缺口")) {
                List<SafetyStockVO> shortages = safety.stream().filter(s -> s.getShortage() > 0).collect(Collectors.toList());
                if (shortages.isEmpty()) return "当前所有耗材库存充足，无需紧急补货。";
                String items = shortages.stream().limit(3)
                        .map(s -> s.getMaterialName() + "（缺口 " + s.getShortage() + "）")
                        .collect(Collectors.joining("、"));
                return String.format("有 %d 种耗材需要补货，缺口最大的：%s。建议尽快创建采购申请。", shortages.size(), items);
            }
            if (anyMatch(msg, "申领", "审批")) {
                if (pending == 0) return "当前无待审批的申领单，申领流程畅通。";
                return String.format("当前有 %d 份申领单待审批，请及时处理以保障科室用耗。", pending);
            }
            if (anyMatch(msg, "库存", "还有多少", "剩余", "剩多少")) {
                // 先尝试从消息中匹配具体耗材名称
                String matched = findMaterialInMessage(msg, safety);
                if (matched != null) {
                    return matched;
                }
                // 无具体耗材，返回整体概况
                long totalMat = materialRepo.count();
                long totalInv = inventoryRepo.count();
                long lowStock = safety.stream().filter(s -> s.getShortage() > 0).count();
                return String.format("当前系统共有 %d 种耗材，%d 个库存批次在管，其中 %d 种低于安全库存。\n如需查询具体耗材库存，请告诉我耗材名称。",
                        totalMat, totalInv, lowStock);
            }
            if (anyMatch(msg, "临期", "过期", "即将到期", "处置")) {
                return "您可以前往「AI 智能 → 预警中心」的「临期处置」标签页查看即将过期耗材及处置建议。";
            }
            if (anyMatch(msg, "消耗异常", "异常消耗", "用量异常")) {
                return "您可以前往「AI 智能 → 预警中心」的「消耗异常」标签页查看近30天的统计异常记录。";
            }
        } catch (Exception e) {
            log.error("规则兜底回复异常：{}", e.getMessage());
        }
        return "我主要负责耗材管理相关问题，您可以问我：\n- 当前有哪些预警？\n- 哪些耗材需要补货？\n- 待审批的申领单有多少？\n- 库存整体状况如何？";
    }

    // ── 工具方法 ──────────────────────────────────────────────────────────────

    /**
     * 在用户消息中模糊匹配耗材名，命中则返回库存回答，未命中返回 null
     */
    private String findMaterialInMessage(String msg, List<SafetyStockVO> safety) {
        for (SafetyStockVO s : safety) {
            String name = s.getMaterialName();
            if (name != null && msg.contains(name)) {
                int stock = s.getCurrentStock();
                String extra = s.getShortage() > 0
                        ? String.format("（低于安全库存，缺口 %d，建议尽快补货）", s.getShortage())
                        : "（库存充足）";
                return String.format("「%s」当前库存 **%d 件** %s", name, stock, extra);
            }
        }
        // 未精确匹配，尝试数据库模糊搜索
        try {
            // 提取可能的耗材关键词（去掉常见停用词）
            String keyword = msg.replaceAll("还有多少|库存|剩余|剩多少|查询|查一下|帮我查|告诉我", "").trim();
            if (keyword.length() >= 2) {
                org.springframework.data.domain.Page<com.medical.system.entity.Material> found =
                    materialRepo.findByConditions(keyword, null, 1,
                        org.springframework.data.domain.PageRequest.of(0, 3));
                if (!found.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (com.medical.system.entity.Material m : found.getContent()) {
                        Integer qty = inventoryRepo.sumQuantityByMaterialId(m.getId());
                        sb.append(String.format("「%s」当前库存 **%d 件**\n", m.getMaterialName(), qty == null ? 0 : qty));
                    }
                    return sb.toString().trim();
                }
            }
        } catch (Exception e) {
            log.warn("耗材模糊搜索失败: {}", e.getMessage());
        }
        return null;
    }

    // ── DTO ──────────────────────────────────────────────────────────────────

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class ChatResponseVO {
        private String reply;
        private ActionVO action;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class ActionVO {
        /** navigate：跳转页面；open_purchase：打开补货 Drawer */
        private String type;
        /** navigate 时的目标路由 */
        private String path;
    }
}
