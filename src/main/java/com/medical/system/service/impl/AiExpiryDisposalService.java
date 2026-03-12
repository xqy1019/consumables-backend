package com.medical.system.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.system.dto.ExpiryDisposalVO;
import com.medical.system.entity.Inventory;
import com.medical.system.entity.Material;
import com.medical.system.entity.AiExpiryDisposalCache;
import com.medical.system.repository.AiExpiryDisposalCacheRepository;
import com.medical.system.repository.InventoryRepository;
import com.medical.system.repository.InventoryTransactionRepository;
import com.medical.system.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiExpiryDisposalService {

    private final InventoryRepository inventoryRepo;
    private final MaterialRepository materialRepo;
    private final InventoryTransactionRepository transactionRepo;
    private final ClaudeService claudeService;
    private final ObjectMapper objectMapper;
    private final AiExpiryDisposalCacheRepository expiryDisposalCacheRepo;

    public List<ExpiryDisposalVO> getDisposalAdvice() {
        LocalDate today = LocalDate.now();
        LocalDate alertDate = today.plusDays(30);

        List<Inventory> expiring = inventoryRepo.findExpiringInventory(alertDate);
        if (expiring.isEmpty()) return Collections.emptyList();

        Set<Long> mids = expiring.stream().map(Inventory::getMaterialId).collect(Collectors.toSet());
        Map<Long, Material> matMap = materialRepo.findAllById(mids)
                .stream().collect(Collectors.toMap(Material::getId, m -> m));

        // 近90天各耗材出库总量 → 日均消耗
        LocalDateTime since90 = LocalDateTime.now().minusDays(90);
        Map<Long, Integer> total90 = new HashMap<>();
        for (Object[] row : transactionRepo.findTotalOutboundByMaterialSince(since90)) {
            total90.put(((Number) row[0]).longValue(), ((Number) row[1]).intValue());
        }

        List<ExpiryDisposalVO> result = new ArrayList<>();
        for (Inventory inv : expiring) {
            Material mat = matMap.get(inv.getMaterialId());
            if (mat == null || inv.getExpiryDate() == null || inv.getQuantity() == null || inv.getQuantity() <= 0)
                continue;

            int daysLeft = (int) ChronoUnit.DAYS.between(today, inv.getExpiryDate());
            if (daysLeft < 0) continue;

            int daily = total90.getOrDefault(inv.getMaterialId(), 0) / 90;
            int estimatedConsumable = daily * daysLeft;
            int riskQty = Math.max(0, inv.getQuantity() - estimatedConsumable);
            if (riskQty <= 0) continue;

            ExpiryDisposalVO vo = new ExpiryDisposalVO();
            vo.setInventoryId(inv.getId());
            vo.setMaterialName(mat.getMaterialName());
            vo.setMaterialCode(mat.getMaterialCode());
            vo.setBatchNumber(inv.getBatchNumber());
            vo.setQuantity(inv.getQuantity());
            vo.setExpiryDate(inv.getExpiryDate());
            vo.setDaysLeft(daysLeft);
            vo.setDailyConsumption(daily);
            vo.setEstimatedConsumable(estimatedConsumable);
            vo.setRiskQuantity(riskQty);
            vo.setAdvice(daysLeft <= 7 ? "DAMAGE" : daysLeft <= 14 ? "RETURN" : "ACCELERATE");
            vo.setReason(daily == 0
                    ? String.format("%d天后预计全部%d件过期，无历史消耗记录", daysLeft, riskQty)
                    : String.format("日均消耗%d件，%d天后预计剩余%d件未使用", daily, daysLeft, riskQty));
            result.add(vo);
        }

        // 构建 inventoryId -> materialId 映射（用于写入缓存）
        Map<Long, Long> invToMatMap = new HashMap<>();
        for (Inventory inv : expiring) {
            invToMatMap.put(inv.getId(), inv.getMaterialId());
        }

        result.sort(Comparator.comparingInt(ExpiryDisposalVO::getDaysLeft));
        enrichWithClaude(result);
        syncToCache(result, invToMatMap);
        return result;
    }

    private void syncToCache(List<ExpiryDisposalVO> items, Map<Long, Long> invToMatMap) {
        try {
            expiryDisposalCacheRepo.deleteAll();
            for (ExpiryDisposalVO vo : items) {
                Long materialId = invToMatMap.get(vo.getInventoryId());
                if (materialId == null) continue; // 找不到 materialId 则跳过，避免违反外键约束
                AiExpiryDisposalCache cache = new AiExpiryDisposalCache();
                cache.setInventoryId(vo.getInventoryId());
                cache.setMaterialId(materialId);
                cache.setAction(vo.getAdvice());
                cache.setReason(vo.getReason() != null && vo.getReason().length() > 100
                        ? vo.getReason().substring(0, 100) : vo.getReason());
                cache.setDaysLeft(vo.getDaysLeft());
                expiryDisposalCacheRepo.save(cache);
            }
            log.info("已同步 {} 条临期处置建议到缓存", items.size());
        } catch (Exception e) {
            log.warn("同步临期处置建议缓存失败: {}", e.getMessage());
        }
    }

    private void enrichWithClaude(List<ExpiryDisposalVO> items) {
        if (items.isEmpty() || !claudeService.isConfigured()) return;
        List<ExpiryDisposalVO> subset = items.stream().limit(10).collect(Collectors.toList());
        try {
            List<Map<String, Object>> data = subset.stream().map(vo -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("materialName", vo.getMaterialName());
                m.put("quantity", vo.getQuantity());
                m.put("daysLeft", vo.getDaysLeft());
                m.put("dailyConsumption", vo.getDailyConsumption());
                m.put("riskQuantity", vo.getRiskQuantity());
                return m;
            }).collect(Collectors.toList());

            String sys = """
                    你是医疗耗材管理专家。以下耗材即将过期且库存无法在效期内消耗完，\
                    给出处置建议（ACCELERATE=加速使用/TRANSFER=跨科调拨/RETURN=联系退货/DAMAGE=办理报损）\
                    和简短理由（≤20字）。\
                    返回纯JSON数组：[{"materialName":"...","advice":"ACCELERATE","reason":"..."}]\
                    不要额外说明。
                    """;
            String resp = claudeService.chatWithSystem(sys, objectMapper.writeValueAsString(data), 800);
            if (resp == null) return;
            List<Map<String, Object>> list = claudeService.extractJsonArray(resp, new TypeReference<List<Map<String, Object>>>() {});
            if (list == null) return;
            Map<String, Map<String, Object>> byName = list.stream()
                    .filter(m -> m.containsKey("materialName"))
                    .collect(Collectors.toMap(m -> m.get("materialName").toString(), m -> m, (a, b) -> a));
            for (ExpiryDisposalVO vo : subset) {
                Map<String, Object> r = byName.get(vo.getMaterialName());
                if (r == null) continue;
                if (r.get("reason") != null) vo.setReason(r.get("reason").toString());
                if (r.get("advice") != null) {
                    String adv = r.get("advice").toString().toUpperCase();
                    if (List.of("ACCELERATE", "TRANSFER", "RETURN", "DAMAGE").contains(adv)) vo.setAdvice(adv);
                }
            }
        } catch (Exception ex) {
            log.warn("临期处置 Claude 失败: {}", ex.getMessage());
        }
    }
}
