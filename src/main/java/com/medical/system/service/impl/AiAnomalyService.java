package com.medical.system.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.system.dto.AnomalyVO;
import com.medical.system.entity.Department;
import com.medical.system.entity.Material;
import com.medical.system.repository.DepartmentRepository;
import com.medical.system.repository.InventoryTransactionRepository;
import com.medical.system.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnomalyService {

    private final InventoryTransactionRepository transactionRepo;
    private final MaterialRepository materialRepo;
    private final DepartmentRepository deptRepo;
    private final ClaudeService claudeService;
    private final ObjectMapper objectMapper;

    public List<AnomalyVO> detectAnomalies() {
        // 90天数据建基线，上报近30天异常
        LocalDateTime since90 = LocalDateTime.now().minusDays(90);
        String cutoff30 = LocalDateTime.now().minusDays(30).toLocalDate().toString();

        List<Object[]> rows = transactionRepo.findDailyOutboundGrouped(since90);
        if (rows.isEmpty()) return Collections.emptyList();

        // 按 (materialId, deptId) 聚合所有日数据
        record Key(long mid, long did) {}
        Map<Key, List<Double>> allDaily = new LinkedHashMap<>();
        Map<Key, Map<String, Integer>> recentDaily = new LinkedHashMap<>();

        for (Object[] row : rows) {
            if (row[0] == null || row[1] == null || row[2] == null || row[3] == null) continue;
            long mid   = ((Number) row[0]).longValue();
            long did   = ((Number) row[1]).longValue();
            String day = row[2].toString();
            int qty    = ((Number) row[3]).intValue();
            Key key = new Key(mid, did);
            allDaily.computeIfAbsent(key, k -> new ArrayList<>()).add((double) qty);
            if (day.compareTo(cutoff30) >= 0) {
                recentDaily.computeIfAbsent(key, k -> new LinkedHashMap<>()).put(day, qty);
            }
        }

        // 加载耗材和科室信息
        Set<Long> mids = allDaily.keySet().stream().map(Key::mid).collect(Collectors.toSet());
        Set<Long> dids = allDaily.keySet().stream().map(Key::did).collect(Collectors.toSet());
        Map<Long, Material>   matMap  = materialRepo.findAllById(mids).stream()
                .collect(Collectors.toMap(Material::getId, m -> m));
        Map<Long, Department> deptMap = deptRepo.findAllById(dids).stream()
                .collect(Collectors.toMap(Department::getId, d -> d));

        List<AnomalyVO> anomalies = new ArrayList<>();

        for (Map.Entry<Key, List<Double>> entry : allDaily.entrySet()) {
            Key key = entry.getKey();
            List<Double> vals = entry.getValue();
            if (vals.size() < 5) continue; // 数据不足，跳过

            double mean   = vals.stream().mapToDouble(d -> d).average().orElse(0);
            double stddev = Math.sqrt(vals.stream().mapToDouble(d -> (d - mean) * (d - mean)).average().orElse(0));
            if (stddev < 1) continue; // 波动极小，正常稳定消耗

            Map<String, Integer> recent = recentDaily.getOrDefault(key, Collections.emptyMap());
            for (Map.Entry<String, Integer> dayEntry : recent.entrySet()) {
                int qty = dayEntry.getValue();
                double ratio = mean > 0 ? qty / mean : 0;
                String severity;
                if      (qty > mean + 3 * stddev) severity = "HIGH";
                else if (qty > mean + 2 * stddev) severity = "MEDIUM";
                else continue;

                Material   mat  = matMap.get(key.mid());
                Department dept = deptMap.get(key.did());
                if (mat == null) continue;

                AnomalyVO vo = new AnomalyVO();
                vo.setMaterialId(key.mid());
                vo.setMaterialName(mat.getMaterialName());
                vo.setMaterialCode(mat.getMaterialCode());
                vo.setDeptId(key.did());
                vo.setDeptName(dept != null ? dept.getDeptName() : "未知科室");
                vo.setAnomalyDate(dayEntry.getKey());
                vo.setAnomalyQuantity(qty);
                vo.setAvgDailyConsumption((int) Math.round(mean));
                vo.setAnomalyRatio(Math.round(ratio * 10.0) / 10.0);
                vo.setSeverity(severity);
                vo.setReason(String.format("当日消耗%d件，是均值%.0f件的%.1f倍", qty, mean, ratio));
                anomalies.add(vo);
            }
        }

        // 按严重度降序、日期降序排列
        anomalies.sort(Comparator
                .comparingInt((AnomalyVO v) -> "HIGH".equals(v.getSeverity()) ? 0 : 1)
                .thenComparing(Comparator.comparing(AnomalyVO::getAnomalyDate).reversed()));

        List<AnomalyVO> top20 = anomalies.stream().limit(20).collect(Collectors.toList());
        enrichWithClaude(top20);
        return top20;
    }

    private void enrichWithClaude(List<AnomalyVO> items) {
        if (items.isEmpty() || !claudeService.isConfigured()) return;
        List<AnomalyVO> subset = items.stream().limit(10).collect(Collectors.toList());
        try {
            List<Map<String, Object>> data = subset.stream().map(vo -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("materialName", vo.getMaterialName());
                m.put("deptName", vo.getDeptName());
                m.put("anomalyDate", vo.getAnomalyDate());
                m.put("anomalyQty", vo.getAnomalyQuantity());
                m.put("avgDaily", vo.getAvgDailyConsumption());
                m.put("ratio", vo.getAnomalyRatio());
                return m;
            }).collect(Collectors.toList());

            String sys = """
                    你是医疗耗材审计专家。以下记录是单日消耗量远超历史均值的异常，\
                    分析可能原因（≤20字，如：批量备货/集中手术/数据录入错误/疑似多领）。\
                    返回纯JSON数组：[{"materialName":"...","deptName":"...","reason":"..."}]\
                    不要额外说明。
                    """;
            String resp = claudeService.chatWithSystem(sys, objectMapper.writeValueAsString(data), 800);
            if (resp == null) return;
            List<Map<String, Object>> list = claudeService.extractJsonArray(resp, new TypeReference<List<Map<String, Object>>>() {});
            if (list == null) return;
            for (AnomalyVO vo : subset) {
                list.stream()
                        .filter(m -> vo.getMaterialName().equals(m.get("materialName"))
                                && vo.getDeptName().equals(m.get("deptName")))
                        .findFirst()
                        .ifPresent(r -> { if (r.get("reason") != null) vo.setReason(r.get("reason").toString()); });
            }
        } catch (Exception ex) {
            log.warn("消耗异常 Claude 失败: {}", ex.getMessage());
        }
    }
}
