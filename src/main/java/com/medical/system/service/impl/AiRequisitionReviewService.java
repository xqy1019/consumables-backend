package com.medical.system.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.system.dto.RequisitionReviewItemVO;
import com.medical.system.entity.Material;
import com.medical.system.entity.RequisitionItem;
import com.medical.system.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiRequisitionReviewService {

    private final RequisitionRepository requisitionRepo;
    private final RequisitionItemRepository requisitionItemRepo;
    private final MaterialRepository materialRepo;
    private final InventoryRepository inventoryRepo;
    private final InventoryTransactionRepository transactionRepo;
    private final ClaudeService claudeService;
    private final ObjectMapper objectMapper;

    public List<RequisitionReviewItemVO> review(Long requisitionId) {
        var requisition = requisitionRepo.findById(requisitionId)
                .orElseThrow(() -> new RuntimeException("申领单不存在"));
        Long deptId = requisition.getDeptId();

        List<RequisitionItem> items = requisitionItemRepo.findByRequisitionId(requisitionId);
        if (items.isEmpty()) return Collections.emptyList();

        Set<Long> mids = items.stream().map(RequisitionItem::getMaterialId).collect(Collectors.toSet());
        Map<Long, Material> matMap = materialRepo.findAllById(mids)
                .stream().collect(Collectors.toMap(Material::getId, m -> m));

        // 近3个月该科室各耗材出库总量
        LocalDateTime since3m = LocalDateTime.now().minusMonths(3);
        Map<Long, Integer> totalMap = new HashMap<>();
        for (Object[] row : transactionRepo.sumOutboundByMaterialForDept(deptId, since3m)) {
            totalMap.put(((Number) row[0]).longValue(), ((Number) row[1]).intValue());
        }

        List<RequisitionReviewItemVO> result = new ArrayList<>();
        for (RequisitionItem item : items) {
            Material mat = matMap.get(item.getMaterialId());
            if (mat == null) continue;

            Integer stock = inventoryRepo.sumQuantityByMaterialId(item.getMaterialId());
            if (stock == null) stock = 0;

            int total3m = totalMap.getOrDefault(item.getMaterialId(), 0);
            int avgMonthly = total3m / 3;
            int requested = item.getQuantity() != null ? item.getQuantity() : 0;

            String verdict;
            if (avgMonthly == 0) {
                verdict = requested > 0 ? "ABNORMAL" : "NORMAL";
            } else {
                double ratio = (double) (requested - avgMonthly) / avgMonthly;
                if (ratio > 0.5)       verdict = "TOO_MUCH";
                else if (ratio < -0.3) verdict = "TOO_LESS";
                else                   verdict = "NORMAL";
            }

            RequisitionReviewItemVO vo = new RequisitionReviewItemVO();
            vo.setMaterialId(item.getMaterialId());
            vo.setMaterialName(mat.getMaterialName());
            vo.setSpecification(mat.getSpecification());
            vo.setUnit(mat.getUnit());
            vo.setRequestedQuantity(requested);
            vo.setAvgMonthlyConsumption(avgMonthly);
            vo.setCurrentStock(stock);
            vo.setVerdict(verdict);
            vo.setReason(defaultReason(verdict, requested, avgMonthly, stock));
            result.add(vo);
        }

        enrichWithClaude(result);
        return result;
    }

    private String defaultReason(String verdict, int req, int avg, int stock) {
        return switch (verdict) {
            case "TOO_MUCH" -> String.format("申领%d件是月均%d件的%.1f倍，建议核实需求", req, avg, avg > 0 ? (double) req / avg : 0);
            case "TOO_LESS" -> String.format("申领%d件低于月均%d件，当前库存%d件", req, avg, stock);
            case "ABNORMAL" -> "历史无消耗记录，请确认是否首次使用";
            default         -> String.format("申领%d件，月均%d件，用量正常", req, avg);
        };
    }

    private void enrichWithClaude(List<RequisitionReviewItemVO> items) {
        if (items.isEmpty() || !claudeService.isConfigured()) return;
        try {
            List<Map<String, Object>> data = items.stream().map(vo -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("materialName", vo.getMaterialName());
                m.put("requested", vo.getRequestedQuantity());
                m.put("avgMonthly", vo.getAvgMonthlyConsumption());
                m.put("currentStock", vo.getCurrentStock());
                return m;
            }).collect(Collectors.toList());

            String sys = """
                    你是医疗耗材申领审批专家。对以下申领明细逐条给出审批意见，\
                    verdict取值：NORMAL/TOO_MUCH/TOO_LESS/ABNORMAL，reason不超过25字。\
                    返回纯JSON数组：[{"materialName":"...","verdict":"NORMAL","reason":"..."}]\
                    不要任何额外说明。
                    """;
            String resp = claudeService.chatWithSystem(sys, objectMapper.writeValueAsString(data), 800);
            if (resp == null) return;
            List<Map<String, Object>> list = claudeService.extractJsonArray(resp, new TypeReference<List<Map<String, Object>>>() {});
            if (list == null) return;
            Map<String, Map<String, Object>> byName = list.stream()
                    .filter(m -> m.containsKey("materialName"))
                    .collect(Collectors.toMap(m -> m.get("materialName").toString(), m -> m, (a, b) -> a));
            for (RequisitionReviewItemVO vo : items) {
                Map<String, Object> r = byName.get(vo.getMaterialName());
                if (r == null) continue;
                if (r.get("reason") != null)  vo.setReason(r.get("reason").toString());
                if (r.get("verdict") != null) {
                    String v = r.get("verdict").toString().toUpperCase();
                    if (List.of("NORMAL", "TOO_MUCH", "TOO_LESS", "ABNORMAL").contains(v)) vo.setVerdict(v);
                }
            }
        } catch (Exception ex) {
            log.warn("审批 review Claude 失败: {}", ex.getMessage());
        }
    }
}
