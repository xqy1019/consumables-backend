package com.medical.system.service.impl;

import com.medical.system.entity.*;
import com.medical.system.exception.BusinessException;
import com.medical.system.repository.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeptInventoryService {

    private final DeptInventoryRepository deptInventoryRepository;
    private final DeptStocktakingRepository stocktakingRepository;
    private final DeptStocktakingItemRepository stocktakingItemRepository;
    private final AutoReplenishmentLogRepository replenishmentLogRepository;
    private final DeptParLevelRepository parLevelRepository;
    private final DepartmentRepository departmentRepository;
    private final MaterialRepository materialRepository;
    private final RequisitionRepository requisitionRepository;
    private final RequisitionItemRepository requisitionItemRepository;

    // ════════════════════════════════════════════════
    //  科室二级库查询
    // ════════════════════════════════════════════════

    /** 获取某科室的二级库存列表 */
    public List<DeptInventoryVO> getDeptInventory(Long deptId) {
        List<DeptInventory> list = deptInventoryRepository.findByDeptId(deptId);
        Map<Long, Material> matMap = materialRepository.findAll().stream()
                .collect(Collectors.toMap(Material::getId, m -> m, (a, b) -> a));
        Map<Long, DeptParLevel> parMap = parLevelRepository.findByDeptIdAndIsActiveTrue(deptId).stream()
                .collect(Collectors.toMap(DeptParLevel::getMaterialId, p -> p, (a, b) -> a));

        return list.stream().map(inv -> {
            DeptInventoryVO vo = new DeptInventoryVO();
            vo.setId(inv.getId());
            vo.setDeptId(inv.getDeptId());
            vo.setMaterialId(inv.getMaterialId());
            vo.setCurrentQuantity(inv.getCurrentQuantity());
            vo.setLastStocktakingAt(inv.getLastStocktakingAt());

            Material m = matMap.get(inv.getMaterialId());
            if (m != null) {
                vo.setMaterialName(m.getMaterialName());
                vo.setMaterialSpec(m.getSpecification());
                vo.setUnit(m.getUnit());
            }

            DeptParLevel par = parMap.get(inv.getMaterialId());
            if (par != null) {
                vo.setParQuantity(par.getParQuantity());
                vo.setMinQuantity(par.getMinQuantity());
                vo.setBelowMin(inv.getCurrentQuantity().compareTo(par.getMinQuantity()) < 0);
            }
            return vo;
        }).collect(Collectors.toList());
    }

    /** 获取所有科室的二级库存概览 */
    public List<DeptInventorySummaryVO> getAllDeptSummary() {
        List<DeptInventory> allInv = deptInventoryRepository.findAll();
        Map<Long, String> deptNames = departmentRepository.findAll().stream()
                .collect(Collectors.toMap(Department::getId, Department::getDeptName, (a, b) -> a));
        Map<Long, List<DeptParLevel>> parByDept = parLevelRepository.findByIsActiveTrue().stream()
                .collect(Collectors.groupingBy(DeptParLevel::getDeptId));

        Map<Long, List<DeptInventory>> byDept = allInv.stream()
                .collect(Collectors.groupingBy(DeptInventory::getDeptId));

        List<DeptInventorySummaryVO> result = new ArrayList<>();
        byDept.forEach((deptId, invList) -> {
            DeptInventorySummaryVO vo = new DeptInventorySummaryVO();
            vo.setDeptId(deptId);
            vo.setDeptName(deptNames.getOrDefault(deptId, "未知科室"));
            vo.setTotalItems(invList.size());

            List<DeptParLevel> pars = parByDept.getOrDefault(deptId, List.of());
            Map<Long, DeptParLevel> parMap = pars.stream()
                    .collect(Collectors.toMap(DeptParLevel::getMaterialId, p -> p, (a, b) -> a));

            int belowMin = 0;
            for (DeptInventory inv : invList) {
                DeptParLevel par = parMap.get(inv.getMaterialId());
                if (par != null && inv.getCurrentQuantity().compareTo(par.getMinQuantity()) < 0) {
                    belowMin++;
                }
            }
            vo.setBelowMinCount(belowMin);
            result.add(vo);
        });
        return result;
    }

    // ════════════════════════════════════════════════
    //  申领发放时更新科室库存（钩子方法）
    // ════════════════════════════════════════════════

    /** 申领发放后，将物资入库到科室二级库 */
    @Transactional
    public void onRequisitionDispatched(Long deptId, List<RequisitionItem> items) {
        for (RequisitionItem item : items) {
            int actualQty = item.getActualQuantity() != null ? item.getActualQuantity() : item.getQuantity();
            DeptInventory inv = deptInventoryRepository.findByDeptIdAndMaterialId(deptId, item.getMaterialId())
                    .orElseGet(() -> {
                        DeptInventory newInv = new DeptInventory();
                        newInv.setDeptId(deptId);
                        newInv.setMaterialId(item.getMaterialId());
                        newInv.setCurrentQuantity(BigDecimal.ZERO);
                        return newInv;
                    });
            inv.setCurrentQuantity(inv.getCurrentQuantity().add(BigDecimal.valueOf(actualQty)));
            deptInventoryRepository.save(inv);
            log.info("科室二级库入库：dept={}, material={}, +{}", deptId, item.getMaterialId(), actualQty);
        }
    }

    // ════════════════════════════════════════════════
    //  科室盘点（消耗倒推核心）
    // ════════════════════════════════════════════════

    /** 创建盘点单：自动加载科室当前所有库存 */
    @Transactional
    public DeptStocktakingVO createStocktaking(Long deptId, Long userId) {
        List<DeptInventory> invList = deptInventoryRepository.findByDeptId(deptId);
        if (invList.isEmpty()) {
            throw new BusinessException("该科室暂无二级库存记录");
        }

        DeptStocktaking st = new DeptStocktaking();
        st.setDeptId(deptId);
        st.setCreatedBy(userId);
        st = stocktakingRepository.save(st);

        List<DeptStocktakingItem> items = new ArrayList<>();
        for (DeptInventory inv : invList) {
            DeptStocktakingItem item = new DeptStocktakingItem();
            item.setStocktakingId(st.getId());
            item.setMaterialId(inv.getMaterialId());
            item.setSystemQuantity(inv.getCurrentQuantity());
            items.add(item);
        }
        stocktakingItemRepository.saveAll(items);

        return buildStocktakingVO(st, items);
    }

    /** 完成盘点：输入实盘数量，自动计算消耗 */
    @Transactional
    public DeptStocktakingVO completeStocktaking(Long stocktakingId, List<StocktakingInput> inputs) {
        DeptStocktaking st = stocktakingRepository.findById(stocktakingId)
                .orElseThrow(() -> new BusinessException("盘点单不存在"));
        if (!"IN_PROGRESS".equals(st.getStatus())) {
            throw new BusinessException("该盘点单已完成");
        }

        List<DeptStocktakingItem> items = stocktakingItemRepository.findByStocktakingId(stocktakingId);
        Map<Long, BigDecimal> inputMap = inputs.stream()
                .collect(Collectors.toMap(StocktakingInput::getMaterialId, StocktakingInput::getActualQuantity));

        BigDecimal totalConsumption = BigDecimal.ZERO;
        for (DeptStocktakingItem item : items) {
            BigDecimal actual = inputMap.get(item.getMaterialId());
            if (actual == null) continue;

            item.setActualQuantity(actual);
            // 消耗 = 系统账面 - 实际盘点（正数=正常消耗，负数=盘盈异常）
            BigDecimal consumed = item.getSystemQuantity().subtract(actual);
            item.setConsumption(consumed);

            if (consumed.compareTo(BigDecimal.ZERO) > 0) {
                totalConsumption = totalConsumption.add(consumed);
            }

            // 更新科室库存为实盘数量
            deptInventoryRepository.findByDeptIdAndMaterialId(st.getDeptId(), item.getMaterialId())
                    .ifPresent(inv -> {
                        inv.setCurrentQuantity(actual);
                        inv.setLastStocktakingAt(LocalDateTime.now());
                        deptInventoryRepository.save(inv);
                    });
        }
        stocktakingItemRepository.saveAll(items);

        st.setStatus("COMPLETED");
        st.setTotalConsumption(totalConsumption);
        st.setCompletedAt(LocalDateTime.now());
        stocktakingRepository.save(st);

        log.info("科室盘点完成：dept={}, 总消耗量={}", st.getDeptId(), totalConsumption);
        return buildStocktakingVO(st, items);
    }

    /** 查询科室盘点历史 */
    public List<DeptStocktakingVO> getStocktakingHistory(Long deptId) {
        List<DeptStocktaking> list = stocktakingRepository.findByDeptIdOrderByCreatedAtDesc(deptId);
        return list.stream().map(st -> {
            List<DeptStocktakingItem> items = stocktakingItemRepository.findByStocktakingId(st.getId());
            return buildStocktakingVO(st, items);
        }).collect(Collectors.toList());
    }

    // ════════════════════════════════════════════════
    //  自动补货检查
    // ════════════════════════════════════════════════

    /** 检查所有科室库存，低于补货线的生成补货建议 */
    public List<ReplenishmentSuggestionVO> checkReplenishment() {
        List<DeptParLevel> parLevels = parLevelRepository.findByIsActiveTrue();
        Map<Long, String> deptNames = departmentRepository.findAll().stream()
                .collect(Collectors.toMap(Department::getId, Department::getDeptName, (a, b) -> a));
        Map<Long, Material> matMap = materialRepository.findAll().stream()
                .collect(Collectors.toMap(Material::getId, m -> m, (a, b) -> a));

        List<ReplenishmentSuggestionVO> suggestions = new ArrayList<>();
        for (DeptParLevel par : parLevels) {
            DeptInventory inv = deptInventoryRepository
                    .findByDeptIdAndMaterialId(par.getDeptId(), par.getMaterialId())
                    .orElse(null);
            BigDecimal current = inv != null ? inv.getCurrentQuantity() : BigDecimal.ZERO;

            if (current.compareTo(par.getMinQuantity()) < 0) {
                ReplenishmentSuggestionVO vo = new ReplenishmentSuggestionVO();
                vo.setDeptId(par.getDeptId());
                vo.setDeptName(deptNames.getOrDefault(par.getDeptId(), "未知"));
                vo.setMaterialId(par.getMaterialId());
                Material m = matMap.get(par.getMaterialId());
                if (m != null) {
                    vo.setMaterialName(m.getMaterialName());
                    vo.setUnit(m.getUnit());
                }
                vo.setCurrentQuantity(current);
                vo.setMinQuantity(par.getMinQuantity());
                vo.setParQuantity(par.getParQuantity());
                vo.setSuggestedQuantity(par.getParQuantity().subtract(current));
                suggestions.add(vo);
            }
        }
        return suggestions;
    }

    /** 执行自动补货：为低库存项生成申领单草稿 */
    @Transactional
    public int executeAutoReplenishment(Long userId) {
        List<ReplenishmentSuggestionVO> suggestions = checkReplenishment();
        if (suggestions.isEmpty()) return 0;

        // 按科室分组生成申领单
        Map<Long, List<ReplenishmentSuggestionVO>> byDept = suggestions.stream()
                .collect(Collectors.groupingBy(ReplenishmentSuggestionVO::getDeptId));

        int count = 0;
        for (var entry : byDept.entrySet()) {
            Long deptId = entry.getKey();
            List<ReplenishmentSuggestionVO> items = entry.getValue();

            // 创建申领单草稿
            Requisition req = new Requisition();
            req.setRequisitionNo("AUTO-" + System.currentTimeMillis() + "-" + deptId);
            req.setDeptId(deptId);
            req.setStatus("DRAFT");
            req.setRemark("系统自动补货：" + items.size() + "项耗材低于补货线");
            req.setCreatedBy(userId);
            req = requisitionRepository.save(req);

            for (ReplenishmentSuggestionVO s : items) {
                RequisitionItem ri = new RequisitionItem();
                ri.setRequisitionId(req.getId());
                ri.setMaterialId(s.getMaterialId());
                ri.setQuantity(s.getSuggestedQuantity().intValue());
                ri.setRemark("自动补货：当前" + s.getCurrentQuantity() + "，低于触发线" + s.getMinQuantity());
                requisitionItemRepository.save(ri);

                // 记录补货日志
                AutoReplenishmentLog logEntry = new AutoReplenishmentLog();
                logEntry.setDeptId(deptId);
                logEntry.setMaterialId(s.getMaterialId());
                logEntry.setCurrentQuantity(s.getCurrentQuantity());
                logEntry.setMinQuantity(s.getMinQuantity());
                logEntry.setReplenishQuantity(s.getSuggestedQuantity());
                logEntry.setRequisitionId(req.getId());
                replenishmentLogRepository.save(logEntry);
            }
            count++;
            log.info("自动补货：为科室{}生成申领单，含{}项耗材", deptId, items.size());
        }
        return count;
    }

    // ════════════════════════════════════════════════
    //  科室消耗排名
    // ════════════════════════════════════════════════

    /** 获取各科室消耗排名（基于已完成盘点） */
    public List<DeptConsumptionRankVO> getDeptConsumptionRanking() {
        List<DeptStocktaking> completed = stocktakingRepository.findAll().stream()
                .filter(st -> "COMPLETED".equals(st.getStatus()))
                .collect(Collectors.toList());

        if (completed.isEmpty()) return List.of();

        Map<Long, String> deptNames = departmentRepository.findAll().stream()
                .collect(Collectors.toMap(Department::getId, Department::getDeptName, (a, b) -> a));

        Map<Long, List<DeptStocktaking>> byDept = completed.stream()
                .collect(Collectors.groupingBy(DeptStocktaking::getDeptId));

        List<DeptConsumptionRankVO> result = new ArrayList<>();
        byDept.forEach((deptId, stocktakings) -> {
            DeptConsumptionRankVO vo = new DeptConsumptionRankVO();
            vo.setDeptId(deptId);
            vo.setDeptName(deptNames.getOrDefault(deptId, "未知科室"));
            vo.setTotalStocktakings(stocktakings.size());

            double total = stocktakings.stream()
                    .mapToDouble(st -> st.getTotalConsumption() != null ? st.getTotalConsumption().doubleValue() : 0)
                    .sum();
            vo.setTotalConsumption(total);
            vo.setAvgConsumption(stocktakings.isEmpty() ? 0 : total / stocktakings.size());

            // 最近一次盘点的消耗
            DeptStocktaking latest = stocktakings.stream()
                    .max(Comparator.comparing(DeptStocktaking::getCompletedAt))
                    .orElse(null);
            vo.setLastConsumption(latest != null && latest.getTotalConsumption() != null
                    ? latest.getTotalConsumption().doubleValue() : 0);

            result.add(vo);
        });

        result.sort(Comparator.comparingDouble(DeptConsumptionRankVO::getTotalConsumption).reversed());
        return result;
    }

    // ════════════════════════════════════════════════
    //  VO & 辅助
    // ════════════════════════════════════════════════

    private DeptStocktakingVO buildStocktakingVO(DeptStocktaking st, List<DeptStocktakingItem> items) {
        Map<Long, Material> matMap = materialRepository.findAll().stream()
                .collect(Collectors.toMap(Material::getId, m -> m, (a, b) -> a));
        Map<Long, String> deptNames = departmentRepository.findAll().stream()
                .collect(Collectors.toMap(Department::getId, Department::getDeptName, (a, b) -> a));

        DeptStocktakingVO vo = new DeptStocktakingVO();
        vo.setId(st.getId());
        vo.setDeptId(st.getDeptId());
        vo.setDeptName(deptNames.getOrDefault(st.getDeptId(), "未知"));
        vo.setStatus(st.getStatus());
        vo.setTotalConsumption(st.getTotalConsumption());
        vo.setCreatedAt(st.getCreatedAt());
        vo.setCompletedAt(st.getCompletedAt());

        vo.setItems(items.stream().map(item -> {
            StocktakingItemVO iv = new StocktakingItemVO();
            iv.setId(item.getId());
            iv.setMaterialId(item.getMaterialId());
            Material m = matMap.get(item.getMaterialId());
            if (m != null) {
                iv.setMaterialName(m.getMaterialName());
                iv.setUnit(m.getUnit());
            }
            iv.setSystemQuantity(item.getSystemQuantity());
            iv.setActualQuantity(item.getActualQuantity());
            iv.setConsumption(item.getConsumption());
            return iv;
        }).collect(Collectors.toList()));
        return vo;
    }

    // ── 内部 VO 定义 ──

    @Data
    public static class DeptInventoryVO {
        private Long id;
        private Long deptId;
        private Long materialId;
        private String materialName;
        private String materialSpec;
        private String unit;
        private BigDecimal currentQuantity;
        private BigDecimal parQuantity;
        private BigDecimal minQuantity;
        private boolean belowMin;
        private LocalDateTime lastStocktakingAt;
    }

    @Data
    public static class DeptInventorySummaryVO {
        private Long deptId;
        private String deptName;
        private int totalItems;
        private int belowMinCount;
    }

    @Data
    public static class DeptStocktakingVO {
        private Long id;
        private Long deptId;
        private String deptName;
        private String status;
        private BigDecimal totalConsumption;
        private LocalDateTime createdAt;
        private LocalDateTime completedAt;
        private List<StocktakingItemVO> items;
    }

    @Data
    public static class StocktakingItemVO {
        private Long id;
        private Long materialId;
        private String materialName;
        private String unit;
        private BigDecimal systemQuantity;
        private BigDecimal actualQuantity;
        private BigDecimal consumption;
    }

    @Data
    public static class StocktakingInput {
        private Long materialId;
        private BigDecimal actualQuantity;
    }

    @Data
    public static class DeptConsumptionRankVO {
        private Long deptId;
        private String deptName;
        private int totalStocktakings;
        private double totalConsumption;
        private double avgConsumption;
        private double lastConsumption;
    }

    @Data
    public static class ReplenishmentSuggestionVO {
        private Long deptId;
        private String deptName;
        private Long materialId;
        private String materialName;
        private String unit;
        private BigDecimal currentQuantity;
        private BigDecimal minQuantity;
        private BigDecimal parQuantity;
        private BigDecimal suggestedQuantity;
    }
}
