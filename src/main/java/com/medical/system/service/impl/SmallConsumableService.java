package com.medical.system.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.medical.system.dto.ConsumptionSummaryVO;
import com.medical.system.entity.*;
import com.medical.system.exception.BusinessException;
import com.medical.system.repository.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmallConsumableService {

    @Value("${app.anomaly.warning-threshold:0.3}")
    private double warningThreshold;

    @Value("${app.anomaly.danger-threshold:0.5}")
    private double dangerThreshold;

    @Value("${app.par-suggestion.safety-margin:1.2}")
    private double safetyMargin;

    @Value("${app.par-suggestion.min-coverage-ratio:0.3}")
    private double minCoverageRatio;

    @Value("${app.par-suggestion.min-deviation:0.15}")
    private double minDeviation;

    private final ClaudeService claudeService;
    private final DeptParLevelRepository parLevelRepository;
    private final ProcedureTemplateRepository templateRepository;
    private final ProcedureTemplateItemRepository templateItemRepository;
    private final ProcedureRecordRepository procedureRecordRepository;
    private final DepartmentRepository departmentRepository;
    private final MaterialRepository materialRepository;
    private final RequisitionRepository requisitionRepository;
    private final RequisitionItemRepository requisitionItemRepository;
    private final AnomalyWorkOrderRepository anomalyWorkOrderRepository;
    private final DeptInventoryRepository deptInventoryRepository;
    private final DeptStocktakingRepository deptStocktakingRepository;
    private final DeptStocktakingItemRepository deptStocktakingItemRepository;

    // ===================== VO 定义 =====================

    @Data
    public static class ParLevelVO {
        private Long id;
        private Long deptId;
        private String deptName;
        private Long materialId;
        private String materialName;
        private String materialSpec;
        private String unit;
        private BigDecimal parQuantity;
        private BigDecimal minQuantity;
        private BigDecimal monthlyLimit;
        private Boolean isActive;
        /** 本月已领用量（从申领单统计） */
        private BigDecimal monthUsed;
        /** 本月限额使用率 0-100，null表示无限额 */
        private Double monthUsageRate;
        /** 是否超限额 */
        private Boolean overLimit;
    }

    @Data
    public static class SaveParLevelRequest {
        private Long deptId;
        private Long materialId;
        private BigDecimal parQuantity;
        private BigDecimal minQuantity;
        private BigDecimal monthlyLimit;
    }

    @Data
    public static class TemplateVO {
        private Long id;
        private String name;
        private String category;
        private String description;
        private Boolean isActive;
        private List<TemplateItemVO> items;
        private LocalDateTime createdAt;
    }

    @Data
    public static class TemplateItemVO {
        private Long id;
        private Long materialId;
        private String materialName;
        private String materialSpec;
        private String unit;
        private BigDecimal quantity;
        private String note;
    }

    @Data
    public static class SaveTemplateRequest {
        private String name;
        private String category;
        private String description;
        private List<TemplateItemRequest> items;
    }

    @Data
    public static class TemplateItemRequest {
        private Long materialId;
        private BigDecimal quantity;
        private String note;
    }

    @Data
    public static class RecordRequest {
        private Long deptId;
        private Long templateId;
        private LocalDateTime performedAt;
        private Integer quantity;
        private String patientInfo;
        private String note;
    }

    @Data
    public static class RecordVO {
        private Long id;
        private Long deptId;
        private String deptName;
        private Long templateId;
        private String templateName;
        private LocalDateTime performedAt;
        private Integer quantity;
        private String patientInfo;
        private String note;
        /** 本次操作消耗的耗材明细 */
        private List<ConsumedItemVO> consumedItems;
    }

    @Data
    public static class ConsumedItemVO {
        private Long materialId;
        private String materialName;
        private String unit;
        private BigDecimal totalQuantity; // 操作次数 × 单次用量
    }

    @Data
    public static class AnomalyVO {
        private Long deptId;
        private String deptName;
        private Long materialId;
        private String materialName;
        private String unit;
        private String yearMonth;           // "2026-03"
        private BigDecimal thisMonthQty;    // 本月领用量
        private BigDecimal baselineQty;     // 基准（前3个月均值）
        private Double deviationRate;       // 偏差率 %，正数=超标
        private BigDecimal monthlyLimit;    // 月度限额（若有）
        private Boolean overLimit;          // 是否超限额
        private String level;               // NORMAL / WARNING / DANGER
    }

    @Data
    public static class AnomalySummaryVO {
        private int totalDepts;
        private int abnormalDepts;
        private int dangerCount;
        private int warningCount;
        private BigDecimal estimatedWaste;  // 超标金额估算
        private List<AnomalyVO> anomalies;
    }

    @Data
    public static class AnomalyTrendVO {
        private String yearMonth;
        private int dangerCount;
        private int warningCount;
        private int totalCount;
    }

    @Data
    public static class AnomalyAnalysisVO {
        private Long deptId;
        private Long materialId;
        private String deptName;
        private String materialName;
        private Double deviationRate;
        private String level;
        private String rootCause;
        private String suggestion;
        private String urgency;
    }

    // ===================== 消耗预测 VO =====================

    @Data
    public static class ConsumptionForecastVO {
        private Long materialId;
        private String materialName;
        private String unit;
        private List<BigDecimal> last3Months;   // 近3个月用量 [最近, 次近, 最远]
        private BigDecimal predictedQty;        // WMA 预测下月需求
        private String trend;                   // UP / DOWN / STABLE
        private String confidence;              // HIGH / MEDIUM / LOW
    }

    // ===================== 科室消耗预测 =====================

    /**
     * 基于加权移动平均法(WMA)预测指定科室下月各耗材需求量。
     * 权重：近3个月 3:2:1
     */
    public List<ConsumptionForecastVO> getConsumptionForecast(Long deptId) {
        YearMonth current = YearMonth.now();

        // 获取最近6个月每种耗材的领用量
        Map<Long, List<BigDecimal>> materialMonthlyData = new LinkedHashMap<>();
        for (int i = 1; i <= 6; i++) {
            YearMonth ym = current.minusMonths(i);
            Map<String, BigDecimal> monthData = calcMonthUsed(ym);
            for (Map.Entry<String, BigDecimal> entry : monthData.entrySet()) {
                String[] parts = entry.getKey().split("_");
                Long entryDeptId = Long.valueOf(parts[0]);
                Long materialId = Long.valueOf(parts[1]);
                if (!entryDeptId.equals(deptId)) continue;
                materialMonthlyData.computeIfAbsent(materialId, k -> {
                    List<BigDecimal> list = new ArrayList<>();
                    for (int j = 0; j < 6; j++) list.add(BigDecimal.ZERO);
                    return list;
                });
                materialMonthlyData.get(materialId).set(i - 1, entry.getValue());
            }
        }

        if (materialMonthlyData.isEmpty()) return List.of();

        Map<Long, Material> materials = loadMaterials(materialMonthlyData.keySet());

        List<ConsumptionForecastVO> result = new ArrayList<>();
        for (Map.Entry<Long, List<BigDecimal>> entry : materialMonthlyData.entrySet()) {
            Long materialId = entry.getKey();
            List<BigDecimal> monthly = entry.getValue();

            int dataMonths = 0;
            for (BigDecimal v : monthly) {
                if (v.compareTo(BigDecimal.ZERO) > 0) dataMonths++;
            }

            List<BigDecimal> last3 = new ArrayList<>();
            last3.add(monthly.get(0));
            last3.add(monthly.get(1));
            last3.add(monthly.get(2));

            // WMA: 权重 3:2:1
            BigDecimal wma = monthly.get(0).multiply(BigDecimal.valueOf(3))
                    .add(monthly.get(1).multiply(BigDecimal.valueOf(2)))
                    .add(monthly.get(2).multiply(BigDecimal.ONE))
                    .divide(BigDecimal.valueOf(6), 1, RoundingMode.HALF_UP);

            // trend: 最近月 vs 3月前
            String trend;
            if (monthly.get(2).compareTo(BigDecimal.ZERO) == 0) {
                trend = monthly.get(0).compareTo(BigDecimal.ZERO) > 0 ? "UP" : "STABLE";
            } else {
                double changeRate = monthly.get(0).subtract(monthly.get(2))
                        .divide(monthly.get(2), 4, RoundingMode.HALF_UP)
                        .doubleValue();
                if (changeRate > 0.2) trend = "UP";
                else if (changeRate < -0.2) trend = "DOWN";
                else trend = "STABLE";
            }

            // confidence
            String confidence;
            if (dataMonths >= 5) {
                double mean = monthly.stream()
                        .filter(v -> v.compareTo(BigDecimal.ZERO) > 0)
                        .mapToDouble(BigDecimal::doubleValue).average().orElse(0);
                double variance = monthly.stream()
                        .filter(v -> v.compareTo(BigDecimal.ZERO) > 0)
                        .mapToDouble(v -> Math.pow(v.doubleValue() - mean, 2))
                        .average().orElse(0);
                double cv = mean > 0 ? Math.sqrt(variance) / mean : 1;
                confidence = cv < 0.3 ? "HIGH" : "MEDIUM";
            } else if (dataMonths >= 3) {
                confidence = "MEDIUM";
            } else {
                confidence = "LOW";
            }

            ConsumptionForecastVO vo = new ConsumptionForecastVO();
            vo.setMaterialId(materialId);
            Material m = materials.get(materialId);
            vo.setMaterialName(m != null ? m.getMaterialName() : "未知耗材");
            vo.setUnit(m != null ? m.getUnit() : "");
            vo.setLast3Months(last3);
            vo.setPredictedQty(wma);
            vo.setTrend(trend);
            vo.setConfidence(confidence);
            result.add(vo);
        }

        result.sort((a, b) -> b.getPredictedQty().compareTo(a.getPredictedQty()));
        return result;
    }

    // ===================== 科室定数管理 =====================

    public List<ParLevelVO> getParLevels(Long deptId) {
        List<DeptParLevel> levels = deptId != null
                ? parLevelRepository.findByDeptIdAndIsActiveTrue(deptId)
                : parLevelRepository.findByIsActiveTrue();

        // 批量加载科室和耗材名称
        Map<Long, String> deptNames = loadDeptNames(levels.stream().map(DeptParLevel::getDeptId).collect(Collectors.toSet()));
        Map<Long, Material> materials = loadMaterials(levels.stream().map(DeptParLevel::getMaterialId).collect(Collectors.toSet()));

        // 计算本月领用量
        YearMonth current = YearMonth.now();
        Map<String, BigDecimal> monthUsed = calcMonthUsed(current);

        return levels.stream().map(l -> {
            ParLevelVO vo = new ParLevelVO();
            vo.setId(l.getId());
            vo.setDeptId(l.getDeptId());
            vo.setDeptName(deptNames.getOrDefault(l.getDeptId(), "未知科室"));
            vo.setMaterialId(l.getMaterialId());
            Material m = materials.get(l.getMaterialId());
            if (m != null) {
                vo.setMaterialName(m.getMaterialName());
                vo.setMaterialSpec(m.getSpecification());
                vo.setUnit(m.getUnit());
            }
            vo.setParQuantity(l.getParQuantity());
            vo.setMinQuantity(l.getMinQuantity());
            vo.setMonthlyLimit(l.getMonthlyLimit());
            vo.setIsActive(l.getIsActive());

            BigDecimal used = monthUsed.getOrDefault(l.getDeptId() + "_" + l.getMaterialId(), BigDecimal.ZERO);
            vo.setMonthUsed(used);

            if (l.getMonthlyLimit() != null && l.getMonthlyLimit().compareTo(BigDecimal.ZERO) > 0) {
                double rate = used.divide(l.getMonthlyLimit(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue();
                vo.setMonthUsageRate(Math.round(rate * 10.0) / 10.0);
                vo.setOverLimit(used.compareTo(l.getMonthlyLimit()) > 0);
            } else {
                vo.setOverLimit(false);
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Transactional
    public ParLevelVO saveParLevel(SaveParLevelRequest req) {
        DeptParLevel level = parLevelRepository
                .findByDeptIdAndMaterialId(req.getDeptId(), req.getMaterialId())
                .orElse(new DeptParLevel());

        level.setDeptId(req.getDeptId());
        level.setMaterialId(req.getMaterialId());
        level.setParQuantity(req.getParQuantity());
        level.setMinQuantity(req.getMinQuantity());
        level.setMonthlyLimit(req.getMonthlyLimit());
        level.setIsActive(true);
        level = parLevelRepository.save(level);

        // 返回完整VO
        final Long savedId = level.getId();
        return getParLevels(req.getDeptId()).stream()
                .filter(v -> v.getId().equals(savedId))
                .findFirst()
                .orElseThrow();
    }

    @Transactional
    public void deleteParLevel(Long id) {
        DeptParLevel level = parLevelRepository.findById(id)
                .orElseThrow(() -> new BusinessException("定数配置不存在"));
        level.setIsActive(false);
        parLevelRepository.save(level);
    }

    // ===================== 诊疗消耗包管理 =====================

    public List<TemplateVO> getTemplates() {
        List<ProcedureTemplate> templates = templateRepository.findByIsActiveTrueOrderByCreatedAtDesc();
        // 手动加载 items（参考项目现有 Requisition 模式）
        templates.forEach(t -> t.setItems(templateItemRepository.findByTemplateId(t.getId())));
        Map<Long, Material> materials = new HashMap<>();
        templates.forEach(t -> t.getItems().forEach(item -> {
            if (!materials.containsKey(item.getMaterialId())) {
                materialRepository.findById(item.getMaterialId())
                        .ifPresent(m -> materials.put(m.getId(), m));
            }
        }));
        return templates.stream().map(t -> toTemplateVO(t, materials)).collect(Collectors.toList());
    }

    @Transactional
    public TemplateVO saveTemplate(SaveTemplateRequest req, Long userId) {
        ProcedureTemplate template = new ProcedureTemplate();
        template.setName(req.getName());
        template.setCategory(req.getCategory());
        template.setDescription(req.getDescription());
        template.setIsActive(true);
        template.setCreatedBy(userId);
        template = templateRepository.save(template);

        if (req.getItems() != null) {
            for (TemplateItemRequest itemReq : req.getItems()) {
                ProcedureTemplateItem item = new ProcedureTemplateItem();
                item.setTemplateId(template.getId());
                item.setMaterialId(itemReq.getMaterialId());
                item.setQuantity(itemReq.getQuantity());
                item.setNote(itemReq.getNote());
                templateItemRepository.save(item);
            }
        }

        // 重新加载完整模板（手动加载 items）
        template = templateRepository.findById(template.getId()).orElseThrow();
        template.setItems(templateItemRepository.findByTemplateId(template.getId()));
        Map<Long, Material> materials = loadMaterialsForTemplate(template);
        return toTemplateVO(template, materials);
    }

    @Transactional
    public TemplateVO updateTemplate(Long id, SaveTemplateRequest req) {
        ProcedureTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new BusinessException("消耗包模板不存在"));
        template.setName(req.getName());
        template.setCategory(req.getCategory());
        template.setDescription(req.getDescription());

        // 替换明细
        templateItemRepository.deleteByTemplateId(id);
        if (req.getItems() != null) {
            for (TemplateItemRequest itemReq : req.getItems()) {
                ProcedureTemplateItem item = new ProcedureTemplateItem();
                item.setTemplateId(id);
                item.setMaterialId(itemReq.getMaterialId());
                item.setQuantity(itemReq.getQuantity());
                item.setNote(itemReq.getNote());
                templateItemRepository.save(item);
            }
        }
        template = templateRepository.save(template);
        template.setItems(templateItemRepository.findByTemplateId(id));
        Map<Long, Material> materials = loadMaterialsForTemplate(template);
        return toTemplateVO(template, materials);
    }

    @Transactional
    public void deleteTemplate(Long id) {
        ProcedureTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new BusinessException("消耗包模板不存在"));
        template.setIsActive(false);
        templateRepository.save(template);
    }

    // ===================== 操作记录管理 =====================

    @Transactional
    public RecordVO addRecord(RecordRequest req, Long userId) {
        ProcedureTemplate template = templateRepository.findById(req.getTemplateId())
                .orElseThrow(() -> new BusinessException("消耗包模板不存在"));

        ProcedureRecord record = new ProcedureRecord();
        record.setDeptId(req.getDeptId());
        record.setTemplateId(req.getTemplateId());
        record.setPerformedBy(userId);
        record.setPerformedAt(req.getPerformedAt() != null ? req.getPerformedAt() : LocalDateTime.now());
        record.setQuantity(req.getQuantity() != null ? req.getQuantity() : 1);
        record.setPatientInfo(req.getPatientInfo());
        record.setNote(req.getNote());
        record = procedureRecordRepository.save(record);

        // 扣减科室二级库存
        List<ProcedureTemplateItem> templateItems = templateItemRepository.findByTemplateId(req.getTemplateId());
        int operationCount = record.getQuantity();
        for (ProcedureTemplateItem item : templateItems) {
            BigDecimal totalConsumption = item.getQuantity().multiply(BigDecimal.valueOf(operationCount));
            deptInventoryRepository.findByDeptIdAndMaterialId(req.getDeptId(), item.getMaterialId())
                    .ifPresent(inv -> {
                        BigDecimal newQty = inv.getCurrentQuantity().subtract(totalConsumption);
                        inv.setCurrentQuantity(newQty.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newQty);
                        deptInventoryRepository.save(inv);
                    });
        }

        return toRecordVO(record, template);
    }

    public List<RecordVO> getRecords(Long deptId, String yearMonth) {
        List<ProcedureRecord> records;
        if (yearMonth != null && !yearMonth.isEmpty()) {
            YearMonth ym = YearMonth.parse(yearMonth);
            LocalDateTime start = ym.atDay(1).atStartOfDay();
            LocalDateTime end = ym.atEndOfMonth().atTime(23, 59, 59);
            records = deptId != null
                    ? procedureRecordRepository.findByDeptIdAndPerformedAtBetweenOrderByPerformedAtDesc(deptId, start, end)
                    : procedureRecordRepository.findByPerformedAtBetween(start, end);
        } else {
            records = deptId != null
                    ? procedureRecordRepository.findByDeptIdOrderByPerformedAtDesc(deptId)
                    : procedureRecordRepository.findAll();
        }

        Map<Long, ProcedureTemplate> templateCache = new HashMap<>();
        return records.stream().map(r -> {
            ProcedureTemplate t = templateCache.computeIfAbsent(r.getTemplateId(),
                    tid -> templateRepository.findById(tid).orElse(null));
            return toRecordVO(r, t);
        }).collect(Collectors.toList());
    }

    // ===================== 消耗异常分析 =====================

    public AnomalySummaryVO getAnomalySummary(String yearMonth) {
        YearMonth target = yearMonth != null ? YearMonth.parse(yearMonth) : YearMonth.now();

        // 本月领用量
        Map<String, BigDecimal> thisMonth = calcMonthUsed(target);

        // 前3个月基准
        Map<String, BigDecimal> baseline = calcBaseline(target, 3);

        // 获取所有定数配置（含月度限额）
        List<DeptParLevel> parLevels = parLevelRepository.findByIsActiveTrue();
        Map<String, BigDecimal> limitMap = parLevels.stream()
                .filter(p -> p.getMonthlyLimit() != null)
                .collect(Collectors.toMap(
                        p -> p.getDeptId() + "_" + p.getMaterialId(),
                        DeptParLevel::getMonthlyLimit,
                        (a, b) -> a
                ));

        Map<Long, String> deptNames = new HashMap<>();
        departmentRepository.findAll().forEach(d -> deptNames.put(d.getId(), d.getDeptName()));

        Map<Long, Material> allMaterials = new HashMap<>();
        materialRepository.findAll().forEach(m -> allMaterials.put(m.getId(), m));

        List<AnomalyVO> anomalies = new ArrayList<>();
        Set<Long> abnormalDepts = new HashSet<>();
        int dangerCount = 0, warningCount = 0;
        BigDecimal estimatedWaste = BigDecimal.ZERO;

        // 遍历本月有领用的科室+耗材组合
        for (Map.Entry<String, BigDecimal> entry : thisMonth.entrySet()) {
            String key = entry.getKey();
            BigDecimal qty = entry.getValue();
            String[] parts = key.split("_");
            Long deptId = Long.valueOf(parts[0]);
            Long materialId = Long.valueOf(parts[1]);

            BigDecimal base = baseline.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal limit = limitMap.get(key);

            // 计算偏差率
            double devRate = 0;
            if (base.compareTo(BigDecimal.ZERO) > 0) {
                devRate = qty.subtract(base)
                        .divide(base, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();
                devRate = Math.round(devRate * 10.0) / 10.0;
            }

            boolean overLimit = limit != null && qty.compareTo(limit) > 0;

            String level = "NORMAL";
            if (base.compareTo(BigDecimal.ZERO) == 0 && qty.compareTo(BigDecimal.ZERO) > 0) {
                // 新耗材：baseline 为 0，本月有用量
                if (limit != null && qty.compareTo(limit) > 0) {
                    // 有月度限额且超出 -> DANGER
                    level = "DANGER";
                    overLimit = true;
                    dangerCount++;
                    abnormalDepts.add(deptId);
                } else {
                    // 计算全院该耗材平均用量
                    BigDecimal totalHospitalQty = BigDecimal.ZERO;
                    int deptCount = 0;
                    for (Map.Entry<String, BigDecimal> other : thisMonth.entrySet()) {
                        if (other.getKey().endsWith("_" + materialId)) {
                            totalHospitalQty = totalHospitalQty.add(other.getValue());
                            deptCount++;
                        }
                    }
                    BigDecimal hospitalAvg = deptCount > 1
                            ? totalHospitalQty.divide(BigDecimal.valueOf(deptCount), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    if (hospitalAvg.compareTo(BigDecimal.ZERO) > 0
                            && qty.compareTo(hospitalAvg.multiply(BigDecimal.valueOf(2))) > 0) {
                        // 超过全院平均用量的 2 倍 -> WARNING
                        level = "WARNING";
                        warningCount++;
                        abnormalDepts.add(deptId);
                    }
                    // 否则 NORMAL，新耗材首月使用，合理用量
                }
            } else if (devRate > dangerThreshold * 100 || overLimit) {
                level = "DANGER";
                dangerCount++;
                abnormalDepts.add(deptId);
            } else if (devRate > warningThreshold * 100) {
                level = "WARNING";
                warningCount++;
                abnormalDepts.add(deptId);
            }

            // 只返回异常数据
            if (!"NORMAL".equals(level)) {
                AnomalyVO vo = new AnomalyVO();
                vo.setDeptId(deptId);
                vo.setDeptName(deptNames.getOrDefault(deptId, "未知科室"));
                vo.setMaterialId(materialId);
                Material m = allMaterials.get(materialId);
                if (m != null) {
                    vo.setMaterialName(m.getMaterialName());
                    vo.setUnit(m.getUnit());
                }
                vo.setYearMonth(target.toString());
                vo.setThisMonthQty(qty);
                vo.setBaselineQty(base.setScale(2, RoundingMode.HALF_UP));
                vo.setDeviationRate(devRate);
                vo.setMonthlyLimit(limit);
                vo.setOverLimit(overLimit);
                vo.setLevel(level);
                anomalies.add(vo);
            }
        }

        // 按危险程度排序
        anomalies.sort(Comparator.comparing(AnomalyVO::getLevel));

        AnomalySummaryVO summary = new AnomalySummaryVO();
        summary.setTotalDepts((int) thisMonth.keySet().stream()
                .map(k -> k.split("_")[0]).distinct().count());
        summary.setAbnormalDepts(abnormalDepts.size());
        summary.setDangerCount(dangerCount);
        summary.setWarningCount(warningCount);
        summary.setEstimatedWaste(estimatedWaste);
        summary.setAnomalies(anomalies);
        return summary;
    }

    // ===================== 异常自动生成工单 =====================

    /**
     * 检测 DANGER 级别异常并自动创建工单（避免重复）。
     * @param yearMonth 目标月份，如 "2026-03"，null 则取当月
     * @param createdByUserId 创建人 ID（API 调用传当前用户，定时任务传系统用户 1）
     * @return 新创建的工单数量
     */
    @Transactional
    public int autoCreateWorkOrdersForAnomalies(String yearMonth, Long createdByUserId) {
        AnomalySummaryVO summary = getAnomalySummary(yearMonth);
        if (summary.getAnomalies() == null || summary.getAnomalies().isEmpty()) {
            return 0;
        }

        int created = 0;
        for (AnomalyVO anomaly : summary.getAnomalies()) {
            if (!"DANGER".equals(anomaly.getLevel())) {
                continue;
            }

            // 检查是否已存在未关闭的同 deptId + materialId 工单
            List<AnomalyWorkOrder> existing = anomalyWorkOrderRepository
                    .findByDeptIdAndMaterialIdAndStatusNot(anomaly.getDeptId(), anomaly.getMaterialId(), "CLOSED");
            if (!existing.isEmpty()) {
                continue;
            }

            AnomalyWorkOrder order = new AnomalyWorkOrder();
            order.setDeptId(anomaly.getDeptId());
            order.setMaterialId(anomaly.getMaterialId());
            order.setAnomalyType("DANGER");
            order.setDeviationRate(anomaly.getDeviationRate());
            order.setStatus("OPEN");
            order.setPriority("HIGH");
            order.setCreatedBy(createdByUserId);
            order.setDescription(String.format(
                    "【自动工单】%s - %s 消耗异常（DANGER）：偏差率%.1f%%，本月用量%s，基准用量%s（%s）",
                    anomaly.getDeptName(),
                    anomaly.getMaterialName(),
                    anomaly.getDeviationRate(),
                    anomaly.getThisMonthQty().toPlainString(),
                    anomaly.getBaselineQty().toPlainString(),
                    anomaly.getYearMonth()
            ));

            anomalyWorkOrderRepository.save(order);
            created++;
        }

        return created;
    }

    // ===================== 异常趋势（同比/环比） =====================

    /**
     * 获取最近 N 个月的异常汇总趋势数据。
     * 循环调用 getAnomalySummary 汇总每月的 dangerCount / warningCount。
     */
    public List<AnomalyTrendVO> getAnomalyTrend(int months) {
        List<AnomalyTrendVO> trend = new ArrayList<>();
        YearMonth now = YearMonth.now();
        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            AnomalySummaryVO summary = getAnomalySummary(ym.toString());
            AnomalyTrendVO vo = new AnomalyTrendVO();
            vo.setYearMonth(ym.toString());
            vo.setDangerCount(summary.getDangerCount());
            vo.setWarningCount(summary.getWarningCount());
            vo.setTotalCount(summary.getDangerCount() + summary.getWarningCount());
            trend.add(vo);
        }
        return trend;
    }

    // ===================== AI 增强异常分析 =====================

    public List<AnomalyAnalysisVO> getAiAnomalyAnalysis(String yearMonth) {
        AnomalySummaryVO summary = getAnomalySummary(yearMonth);
        List<AnomalyVO> anomalies = summary.getAnomalies();
        if (anomalies == null || anomalies.isEmpty()) {
            return List.of();
        }

        // 按 DANGER 优先、偏差率降序排列，限制最多 20 条发送给 AI
        anomalies.sort(Comparator
                .comparing((AnomalyVO a) -> "DANGER".equals(a.getLevel()) ? 0 : 1)
                .thenComparing(a -> a.getDeviationRate() != null ? -a.getDeviationRate() : 0.0));
        List<AnomalyVO> limitedAnomalies = anomalies.size() > 20 ? anomalies.subList(0, 20) : anomalies;

        // 构建基础分析结果列表
        List<AnomalyAnalysisVO> result = limitedAnomalies.stream().map(a -> {
            AnomalyAnalysisVO vo = new AnomalyAnalysisVO();
            vo.setDeptId(a.getDeptId());
            vo.setMaterialId(a.getMaterialId());
            vo.setDeptName(a.getDeptName());
            vo.setMaterialName(a.getMaterialName());
            vo.setDeviationRate(a.getDeviationRate());
            vo.setLevel(a.getLevel());
            return vo;
        }).collect(Collectors.toList());

        // 尝试调用 Claude AI 分析
        if (claudeService.isConfigured()) {
            try {
                StringBuilder prompt = new StringBuilder("以下是本月消耗异常数据，请逐条分析根因并给出建议：\n");
                for (AnomalyVO a : limitedAnomalies) {
                    prompt.append(String.format(
                            "- 科室ID=%d(%s), 耗材ID=%d(%s), 本月用量=%s, 基准用量=%s, 偏差率=%.1f%%, 等级=%s, 超限额=%s\n",
                            a.getDeptId(), a.getDeptName(),
                            a.getMaterialId(), a.getMaterialName(),
                            a.getThisMonthQty(), a.getBaselineQty(),
                            a.getDeviationRate(), a.getLevel(),
                            Boolean.TRUE.equals(a.getOverLimit()) ? "是" : "否"
                    ));
                }

                String aiResponse = claudeService.chatWithSystem(
                        AiPromptTemplates.ANOMALY_ANALYSIS_SYSTEM, prompt.toString(), 1024);

                if (aiResponse != null) {
                    List<AnomalyAnalysisVO> aiResults = claudeService.extractJsonArray(
                            aiResponse, new TypeReference<List<AnomalyAnalysisVO>>() {});
                    if (aiResults != null && !aiResults.isEmpty()) {
                        // 按 (deptId, materialId) 合并 AI 分析结果
                        Map<String, AnomalyAnalysisVO> aiMap = aiResults.stream()
                                .filter(ai -> ai.getDeptId() != null && ai.getMaterialId() != null)
                                .collect(Collectors.toMap(
                                        ai -> ai.getDeptId() + "_" + ai.getMaterialId(),
                                        ai -> ai, (a, b) -> a));
                        for (AnomalyAnalysisVO vo : result) {
                            AnomalyAnalysisVO ai = aiMap.get(vo.getDeptId() + "_" + vo.getMaterialId());
                            if (ai != null) {
                                vo.setRootCause(ai.getRootCause());
                                vo.setSuggestion(ai.getSuggestion());
                                vo.setUrgency(ai.getUrgency());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("AI异常分析调用失败，使用规则兜底: {}", e.getMessage());
            }
        }

        // 规则兜底：对未被 AI 填充的条目使用默认规则
        for (AnomalyAnalysisVO vo : result) {
            if (vo.getRootCause() == null || vo.getRootCause().isBlank()) {
                applyRuleFallback(vo);
            }
        }

        return result;
    }

    private void applyRuleFallback(AnomalyAnalysisVO vo) {
        if (vo.getDeviationRate() != null && vo.getDeviationRate() > 100) {
            vo.setRootCause("用量异常增大，疑似批量领用或记录错误");
            vo.setSuggestion("立即核查领用记录，确认是否为误操作");
            vo.setUrgency("HIGH");
        } else if ("DANGER".equals(vo.getLevel())) {
            vo.setRootCause("已超月度限额，需核查领用合理性");
            vo.setSuggestion("联系科室护士长确认领用原因");
            vo.setUrgency("HIGH");
        } else if (vo.getDeviationRate() != null && vo.getDeviationRate() > 50) {
            vo.setRootCause("消耗量显著偏高，建议核实近期业务量");
            vo.setSuggestion("对比近期门诊/手术量，排除合理增长");
            vo.setUrgency("MEDIUM");
        } else {
            vo.setRootCause("轻微偏高，可能为正常波动");
            vo.setSuggestion("持续监控，暂无需干预");
            vo.setUrgency("LOW");
        }
    }

    // ===================== 私有工具方法 =====================

    /** 计算指定月份各科室各耗材的领用量 */
    private Map<String, BigDecimal> calcMonthUsed(YearMonth ym) {
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.atEndOfMonth().atTime(23, 59, 59);

        // 查询状态为 DISPATCHED 的申领单（已实际发出的）
        List<Requisition> reqs = requisitionRepository.findByStatusAndCreateTimeBetween("DISPATCHED", start, end);

        Map<String, BigDecimal> result = new HashMap<>();
        for (Requisition r : reqs) {
            List<RequisitionItem> items = requisitionItemRepository.findByRequisitionId(r.getId());
            for (RequisitionItem item : items) {
                int qty = item.getActualQuantity() != null ? item.getActualQuantity() : item.getQuantity();
                String key = r.getDeptId() + "_" + item.getMaterialId();
                result.merge(key, BigDecimal.valueOf(qty), BigDecimal::add);
            }
        }
        return result;
    }

    /** 计算前N个月的平均领用量作为基准 */
    private Map<String, BigDecimal> calcBaseline(YearMonth target, int months) {
        Map<String, BigDecimal> total = new HashMap<>();
        Map<String, Integer> counts = new HashMap<>();

        for (int i = 1; i <= months; i++) {
            YearMonth ym = target.minusMonths(i);
            Map<String, BigDecimal> monthData = calcMonthUsed(ym);
            monthData.forEach((k, v) -> {
                total.merge(k, v, BigDecimal::add);
                counts.merge(k, 1, Integer::sum);
            });
        }

        Map<String, BigDecimal> baseline = new HashMap<>();
        total.forEach((k, v) -> {
            int cnt = counts.getOrDefault(k, 1);
            baseline.put(k, v.divide(BigDecimal.valueOf(cnt), 2, RoundingMode.HALF_UP));
        });
        return baseline;
    }

    private Map<Long, String> loadDeptNames(Set<Long> deptIds) {
        Map<Long, String> map = new HashMap<>();
        departmentRepository.findAllById(deptIds).forEach(d -> map.put(d.getId(), d.getDeptName()));
        return map;
    }

    private Map<Long, Material> loadMaterials(Set<Long> ids) {
        Map<Long, Material> map = new HashMap<>();
        materialRepository.findAllById(ids).forEach(m -> map.put(m.getId(), m));
        return map;
    }

    private Map<Long, Material> loadMaterialsForTemplate(ProcedureTemplate t) {
        Map<Long, Material> map = new HashMap<>();
        if (t.getItems() != null) {
            Set<Long> ids = t.getItems().stream().map(ProcedureTemplateItem::getMaterialId).collect(Collectors.toSet());
            materialRepository.findAllById(ids).forEach(m -> map.put(m.getId(), m));
        }
        return map;
    }

    private TemplateVO toTemplateVO(ProcedureTemplate t, Map<Long, Material> materials) {
        TemplateVO vo = new TemplateVO();
        vo.setId(t.getId());
        vo.setName(t.getName());
        vo.setCategory(t.getCategory());
        vo.setDescription(t.getDescription());
        vo.setIsActive(t.getIsActive());
        vo.setCreatedAt(t.getCreatedAt());
        if (t.getItems() != null) {
            vo.setItems(t.getItems().stream().map(item -> {
                TemplateItemVO iv = new TemplateItemVO();
                iv.setId(item.getId());
                iv.setMaterialId(item.getMaterialId());
                Material m = materials.get(item.getMaterialId());
                if (m != null) {
                    iv.setMaterialName(m.getMaterialName());
                    iv.setMaterialSpec(m.getSpecification());
                    iv.setUnit(m.getUnit());
                }
                iv.setQuantity(item.getQuantity());
                iv.setNote(item.getNote());
                return iv;
            }).collect(Collectors.toList()));
        }
        return vo;
    }

    private RecordVO toRecordVO(ProcedureRecord r, ProcedureTemplate template) {
        RecordVO vo = new RecordVO();
        vo.setId(r.getId());
        vo.setDeptId(r.getDeptId());
        departmentRepository.findById(r.getDeptId()).ifPresent(d -> vo.setDeptName(d.getDeptName()));
        vo.setTemplateId(r.getTemplateId());
        if (template != null) {
            vo.setTemplateName(template.getName());
            // 计算本次消耗明细
            if (template.getItems() != null) {
                Map<Long, Material> mats = loadMaterialsForTemplate(template);
                List<ConsumedItemVO> consumed = template.getItems().stream().map(item -> {
                    ConsumedItemVO c = new ConsumedItemVO();
                    c.setMaterialId(item.getMaterialId());
                    Material m = mats.get(item.getMaterialId());
                    if (m != null) {
                        c.setMaterialName(m.getMaterialName());
                        c.setUnit(m.getUnit());
                    }
                    c.setTotalQuantity(item.getQuantity()
                            .multiply(BigDecimal.valueOf(r.getQuantity())));
                    return c;
                }).collect(Collectors.toList());
                vo.setConsumedItems(consumed);
            }
        }
        vo.setPerformedAt(r.getPerformedAt());
        vo.setQuantity(r.getQuantity());
        vo.setPatientInfo(r.getPatientInfo());
        vo.setNote(r.getNote());
        return vo;
    }

    // ===================== 定数智能建议 =====================

    @Data
    public static class ParSuggestionVO {
        private Long deptId;
        private String deptName;
        private Long materialId;
        private String materialName;
        private String unit;
        private Integer currentPar;       // 当前定数量
        private Integer currentMin;       // 当前最低库存
        private Integer suggestedPar;     // 建议定数量
        private Integer suggestedMin;     // 建议最低库存
        private Double avgMonthlyUsage;   // 月均消耗量
        private Double maxMonthlyUsage;   // 最大月消耗量
        private String reason;            // 调整原因
        private String direction;         // UP / DOWN / KEEP
    }

    /**
     * 基于近3个月消耗数据，为所有科室定数配置生成智能调整建议。
     * 算法：
     *   suggestedPar = ceil(avgMonthly * safetyMargin)  覆盖正常波动+安全边际
     *   suggestedMin = ceil(avgMonthly * minCoverageRatio)  约覆盖紧急用量
     *   若当前par与建议偏差>minDeviation，则建议调整
     */
    public List<ParSuggestionVO> getParSuggestions() {
        List<DeptParLevel> parLevels = parLevelRepository.findAll();
        if (parLevels.isEmpty()) return List.of();

        Map<Long, String> deptNames = departmentRepository.findAll().stream()
                .collect(Collectors.toMap(Department::getId, Department::getDeptName, (a, b) -> a));
        Map<Long, Material> matMap = materialRepository.findAll().stream()
                .collect(Collectors.toMap(Material::getId, m -> m, (a, b) -> a));

        // 计算过去3个月每个(deptId, materialId)的月消耗量
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        List<Requisition> recentReqs = requisitionRepository.findAll().stream()
                .filter(r -> "DISPATCHED".equals(r.getStatus()) || "SIGNED".equals(r.getStatus()))
                .filter(r -> r.getRequisitionDate() != null && r.getRequisitionDate().isAfter(threeMonthsAgo))
                .collect(Collectors.toList());

        // (deptId, materialId) -> List<月份用量>
        Map<String, Map<String, Integer>> usageMap = new HashMap<>(); // key -> {month -> qty}
        for (Requisition req : recentReqs) {
            String month = req.getRequisitionDate().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
            List<RequisitionItem> items = requisitionItemRepository.findByRequisitionId(req.getId());
            for (RequisitionItem item : items) {
                String key = req.getDeptId() + "_" + item.getMaterialId();
                usageMap.computeIfAbsent(key, k -> new HashMap<>())
                        .merge(month, item.getActualQuantity() != null ? item.getActualQuantity() : item.getQuantity(), Integer::sum);
            }
        }

        List<ParSuggestionVO> suggestions = new ArrayList<>();
        for (DeptParLevel pl : parLevels) {
            if (!Boolean.TRUE.equals(pl.getIsActive())) continue;

            String key = pl.getDeptId() + "_" + pl.getMaterialId();
            Map<String, Integer> monthlyUsage = usageMap.getOrDefault(key, Map.of());

            if (monthlyUsage.isEmpty()) continue; // 无消耗数据，跳过

            double avg = monthlyUsage.values().stream().mapToInt(Integer::intValue).average().orElse(0);
            int max = monthlyUsage.values().stream().mapToInt(Integer::intValue).max().orElse(0);

            int sugPar = (int) Math.ceil(avg * safetyMargin);
            int sugMin = (int) Math.ceil(avg * minCoverageRatio);

            int curPar = pl.getParQuantity() != null ? pl.getParQuantity().intValue() : 0;
            int curMin = pl.getMinQuantity() != null ? pl.getMinQuantity().intValue() : 0;

            // 只在偏差>15%时建议
            double parDiff = curPar > 0 ? Math.abs(sugPar - curPar) / (double) curPar : 1.0;
            if (parDiff < minDeviation) continue;

            ParSuggestionVO vo = new ParSuggestionVO();
            vo.setDeptId(pl.getDeptId());
            vo.setDeptName(deptNames.getOrDefault(pl.getDeptId(), "未知科室"));
            vo.setMaterialId(pl.getMaterialId());
            Material mat = matMap.get(pl.getMaterialId());
            vo.setMaterialName(mat != null ? mat.getMaterialName() : "未知耗材");
            vo.setUnit(mat != null ? mat.getUnit() : "");
            vo.setCurrentPar(curPar);
            vo.setCurrentMin(curMin);
            vo.setSuggestedPar(sugPar);
            vo.setSuggestedMin(sugMin);
            vo.setAvgMonthlyUsage(Math.round(avg * 10) / 10.0);
            vo.setMaxMonthlyUsage((double) max);

            if (sugPar > curPar) {
                vo.setDirection("UP");
                vo.setReason(String.format("月均消耗%.0f，当前定数%d偏低，建议上调至%d", avg, curPar, sugPar));
            } else {
                vo.setDirection("DOWN");
                vo.setReason(String.format("月均消耗%.0f，当前定数%d偏高，建议下调至%d", avg, curPar, sugPar));
            }

            suggestions.add(vo);
        }

        // 按偏差幅度倒序
        suggestions.sort((a, b) -> Double.compare(
                Math.abs(b.getSuggestedPar() - b.getCurrentPar()),
                Math.abs(a.getSuggestedPar() - a.getCurrentPar())));

        return suggestions;
    }

    /** 一键应用建议：更新定数配置 */
    @Transactional
    public void applyParSuggestion(Long deptId, Long materialId, int newPar, int newMin) {
        DeptParLevel pl = parLevelRepository.findByDeptIdAndMaterialId(deptId, materialId)
                .orElseThrow(() -> new BusinessException("定数配置不存在"));
        pl.setParQuantity(BigDecimal.valueOf(newPar));
        pl.setMinQuantity(BigDecimal.valueOf(newMin));
        parLevelRepository.save(pl);
    }

    // ============ 消耗汇总（自动生成） ============

    /**
     * 根据申领单和盘点数据自动生成消耗汇总记录，无需护士手动录入。
     * 优先使用盘点推算消耗，其次使用申领量作为估算。
     */
    public List<ConsumptionSummaryVO> getConsumptionSummary(Long deptId, String yearMonth) {
        YearMonth ym = yearMonth != null ? YearMonth.parse(yearMonth) : YearMonth.now();
        LocalDateTime monthStart = ym.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = ym.atEndOfMonth().atTime(23, 59, 59);
        String ymStr = ym.toString();

        // 1. 查询本月 DISPATCHED/SIGNED 申领单
        List<String> statuses = List.of("DISPATCHED", "SIGNED");
        List<Requisition> requisitions;
        if (deptId != null) {
            requisitions = requisitionRepository.findByDeptIdAndStatusInAndRequisitionDateBetween(
                    deptId, statuses, monthStart, monthEnd);
        } else {
            requisitions = requisitionRepository.findByStatusInAndRequisitionDateBetween(
                    statuses, monthStart, monthEnd);
        }

        // 2. 按 (deptId, materialId) 汇总申领量
        // key = "deptId:materialId"
        Map<String, BigDecimal> reqQtyMap = new HashMap<>();
        Map<String, Long> keyDeptMap = new HashMap<>();
        Map<String, Long> keyMaterialMap = new HashMap<>();

        if (!requisitions.isEmpty()) {
            List<Long> reqIds = requisitions.stream().map(Requisition::getId).toList();
            Map<Long, Long> reqDeptMap = requisitions.stream()
                    .collect(Collectors.toMap(Requisition::getId, Requisition::getDeptId));
            List<RequisitionItem> items = requisitionItemRepository.findByRequisitionIdIn(reqIds);

            for (RequisitionItem item : items) {
                Long rDeptId = reqDeptMap.get(item.getRequisitionId());
                if (rDeptId == null) continue;
                String key = rDeptId + ":" + item.getMaterialId();
                int qty = item.getActualQuantity() != null ? item.getActualQuantity() : item.getQuantity();
                reqQtyMap.merge(key, BigDecimal.valueOf(qty), BigDecimal::add);
                keyDeptMap.put(key, rDeptId);
                keyMaterialMap.put(key, item.getMaterialId());
            }
        }

        // 3. 查询本月已完成的盘点记录
        List<DeptStocktaking> stocktakings;
        if (deptId != null) {
            stocktakings = deptStocktakingRepository.findByDeptIdAndStatusAndCompletedAtBetween(
                    deptId, "COMPLETED", monthStart, monthEnd);
        } else {
            stocktakings = deptStocktakingRepository.findByStatusAndCompletedAtBetween(
                    "COMPLETED", monthStart, monthEnd);
        }

        // 4. 按 (deptId, materialId) 汇总盘点消耗
        Map<String, BigDecimal> stockConsMap = new HashMap<>();
        if (!stocktakings.isEmpty()) {
            Map<Long, Long> stDeptMap = stocktakings.stream()
                    .collect(Collectors.toMap(DeptStocktaking::getId, DeptStocktaking::getDeptId));
            List<Long> stIds = stocktakings.stream().map(DeptStocktaking::getId).toList();
            List<DeptStocktakingItem> stItems = deptStocktakingItemRepository.findByStocktakingIdIn(stIds);

            for (DeptStocktakingItem si : stItems) {
                Long sDeptId = stDeptMap.get(si.getStocktakingId());
                if (sDeptId == null || si.getConsumption() == null) continue;
                String key = sDeptId + ":" + si.getMaterialId();
                stockConsMap.merge(key, si.getConsumption(), BigDecimal::add);
                keyDeptMap.put(key, sDeptId);
                keyMaterialMap.put(key, si.getMaterialId());
            }
        }

        // 5. 合并所有 key，构建结果
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(reqQtyMap.keySet());
        allKeys.addAll(stockConsMap.keySet());

        if (allKeys.isEmpty()) {
            return List.of();
        }

        // 预加载科室和物资名称
        Set<Long> allDeptIds = new HashSet<>(keyDeptMap.values());
        Set<Long> allMaterialIds = new HashSet<>(keyMaterialMap.values());
        Map<Long, Department> deptMap = departmentRepository.findAllById(allDeptIds).stream()
                .collect(Collectors.toMap(Department::getId, d -> d));
        Map<Long, Material> matMap = materialRepository.findAllById(allMaterialIds).stream()
                .collect(Collectors.toMap(Material::getId, m -> m));

        List<ConsumptionSummaryVO> result = new ArrayList<>();
        for (String key : allKeys) {
            Long kDeptId = keyDeptMap.get(key);
            Long kMaterialId = keyMaterialMap.get(key);
            BigDecimal reqQty = reqQtyMap.get(key);
            BigDecimal stCons = stockConsMap.get(key);

            ConsumptionSummaryVO vo = new ConsumptionSummaryVO();
            vo.setDeptId(kDeptId);
            vo.setMaterialId(kMaterialId);
            vo.setYearMonth(ymStr);
            vo.setRequisitionQuantity(reqQty);
            vo.setStocktakingConsumption(stCons);

            if (stCons != null) {
                vo.setEstimatedConsumption(stCons);
                vo.setSource("盘点修正");
            } else {
                vo.setEstimatedConsumption(reqQty);
                vo.setSource("申领推算");
            }

            Department dept = deptMap.get(kDeptId);
            if (dept != null) {
                vo.setDeptName(dept.getDeptName());
            }

            Material mat = matMap.get(kMaterialId);
            if (mat != null) {
                vo.setMaterialName(mat.getMaterialName());
                vo.setSpecification(mat.getSpecification());
                vo.setUnit(mat.getUnit());
            }

            result.add(vo);
        }

        // 按科室、物资名排序
        result.sort(Comparator.comparing(ConsumptionSummaryVO::getDeptName, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ConsumptionSummaryVO::getMaterialName, Comparator.nullsLast(Comparator.naturalOrder())));

        return result;
    }
}
