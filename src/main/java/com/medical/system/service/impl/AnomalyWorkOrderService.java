package com.medical.system.service.impl;

import com.medical.system.common.PageResult;
import com.medical.system.entity.*;
import com.medical.system.exception.BusinessException;
import com.medical.system.exception.ResourceNotFoundException;
import com.medical.system.repository.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnomalyWorkOrderService {

    private final AnomalyWorkOrderRepository workOrderRepository;
    private final AnomalyWorkOrderCommentRepository commentRepository;
    private final DepartmentRepository departmentRepository;
    private final MaterialRepository materialRepository;
    private final UserRepository userRepository;

    @Value("${app.sla.high-priority-hours:24}")
    private int highPriorityHours;

    @Value("${app.sla.normal-priority-hours:48}")
    private int normalPriorityHours;

    @Value("${app.sla.low-priority-hours:72}")
    private int lowPriorityHours;

    // ════════════════════════════════════════════════
    //  工单查询
    // ════════════════════════════════════════════════

    /** 获取所有工单（分页，按创建时间倒序） */
    public PageResult<WorkOrderVO> getAllWorkOrdersPaged(Pageable pageable) {
        Page<AnomalyWorkOrder> page = workOrderRepository.findAllByOrderByCreatedAtDesc(pageable);
        Map<Long, String> deptNames = buildDeptNameMap();
        Map<Long, String> matNames = buildMaterialNameMap();
        Map<Long, String> userNames = buildUserNameMap();
        List<WorkOrderVO> records = page.getContent().stream()
                .map(o -> convertToVO(o, deptNames, matNames, userNames))
                .collect(Collectors.toList());
        return PageResult.of(records, page.getTotalElements(), pageable.getPageNumber() + 1, pageable.getPageSize());
    }

    /** 按科室查询工单（分页） */
    public PageResult<WorkOrderVO> getWorkOrdersByDeptPaged(Long deptId, Pageable pageable) {
        Page<AnomalyWorkOrder> page = workOrderRepository.findByDeptIdOrderByCreatedAtDesc(deptId, pageable);
        Map<Long, String> deptNames = buildDeptNameMap();
        Map<Long, String> matNames = buildMaterialNameMap();
        Map<Long, String> userNames = buildUserNameMap();
        List<WorkOrderVO> records = page.getContent().stream()
                .map(o -> convertToVO(o, deptNames, matNames, userNames))
                .collect(Collectors.toList());
        return PageResult.of(records, page.getTotalElements(), pageable.getPageNumber() + 1, pageable.getPageSize());
    }

    /** 带多条件筛选的分页查询 */
    public PageResult<WorkOrderVO> getWorkOrdersFiltered(Long deptId, String priority, String status, Pageable pageable) {
        Specification<AnomalyWorkOrder> spec = Specification.where(null);
        if (deptId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("deptId"), deptId));
        }
        if (priority != null && !priority.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), priority));
        }
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        Page<AnomalyWorkOrder> page = workOrderRepository.findAll(spec,
                org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                        Sort.by(Sort.Direction.DESC, "createdAt")));
        Map<Long, String> deptNames = buildDeptNameMap();
        Map<Long, String> matNames = buildMaterialNameMap();
        Map<Long, String> userNames = buildUserNameMap();
        List<WorkOrderVO> records = page.getContent().stream()
                .map(o -> convertToVO(o, deptNames, matNames, userNames))
                .collect(Collectors.toList());
        return PageResult.of(records, page.getTotalElements(), pageable.getPageNumber() + 1, pageable.getPageSize());
    }

    /** 获取所有工单（不分页，供导出使用） */
    public List<WorkOrderVO> getAllWorkOrders() {
        List<AnomalyWorkOrder> orders = workOrderRepository.findAllByOrderByCreatedAtDesc();
        Map<Long, String> deptNames = buildDeptNameMap();
        Map<Long, String> matNames = buildMaterialNameMap();
        Map<Long, String> userNames = buildUserNameMap();
        return orders.stream().map(o -> convertToVO(o, deptNames, matNames, userNames)).collect(Collectors.toList());
    }

    /** 获取单个工单详情（含评论） */
    public WorkOrderVO getWorkOrderById(Long id) {
        AnomalyWorkOrder order = workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("工单", id));
        WorkOrderVO vo = convertToVO(order);

        // 加载评论
        List<AnomalyWorkOrderComment> comments = commentRepository.findByWorkOrderIdOrderByCreatedAtAsc(id);
        Map<Long, String> userNames = buildUserNameMap();
        vo.setComments(comments.stream().map(c -> {
            CommentVO cv = new CommentVO();
            cv.setId(c.getId());
            cv.setUserId(c.getUserId());
            cv.setUserName(userNames.getOrDefault(c.getUserId(), "未知用户"));
            cv.setContent(c.getContent());
            cv.setCreatedAt(c.getCreatedAt());
            return cv;
        }).collect(Collectors.toList()));

        return vo;
    }

    // ════════════════════════════════════════════════
    //  工单操作
    // ════════════════════════════════════════════════

    /** 创建工单 */
    @Transactional
    public WorkOrderVO createWorkOrder(CreateWorkOrderInput input, Long userId) {
        AnomalyWorkOrder order = new AnomalyWorkOrder();
        order.setDeptId(input.getDeptId());
        order.setMaterialId(input.getMaterialId());
        order.setAnomalyType(input.getAnomalyType());
        order.setDeviationRate(input.getDeviationRate());
        order.setDescription(input.getDescription());
        order.setPriority(input.getPriority() != null ? input.getPriority() : "MEDIUM");
        order.setStatus("OPEN");
        order.setCreatedBy(userId);
        order = workOrderRepository.save(order);
        log.info("创建异常工单：id={}, dept={}, material={}, type={}", order.getId(), input.getDeptId(), input.getMaterialId(), input.getAnomalyType());
        return convertToVO(order);
    }

    /** 分配工单 */
    @Transactional
    public WorkOrderVO assignWorkOrder(Long id, Long assigneeId) {
        AnomalyWorkOrder order = workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("工单", id));
        order.setAssignedTo(assigneeId);
        order.setStatus("IN_PROGRESS");
        order = workOrderRepository.save(order);
        log.info("分配异常工单：id={}, assignedTo={}", id, assigneeId);
        return convertToVO(order);
    }

    /** 解决工单 */
    @Transactional
    public WorkOrderVO resolveWorkOrder(Long id, String resolution) {
        AnomalyWorkOrder order = workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("工单", id));
        order.setStatus("RESOLVED");
        order.setResolution(resolution);
        order.setResolvedAt(LocalDateTime.now());
        order = workOrderRepository.save(order);
        log.info("解决异常工单：id={}", id);
        return convertToVO(order);
    }

    /** 关闭工单 */
    @Transactional
    public WorkOrderVO closeWorkOrder(Long id) {
        AnomalyWorkOrder order = workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("工单", id));
        order.setStatus("CLOSED");
        order.setClosedAt(LocalDateTime.now());
        order = workOrderRepository.save(order);
        log.info("关闭异常工单：id={}", id);
        return convertToVO(order);
    }

    /** 批量分配工单 */
    @Transactional
    public int batchAssign(List<Long> ids, Long assigneeId) {
        List<AnomalyWorkOrder> orders = workOrderRepository.findAllById(ids);
        int count = 0;
        for (AnomalyWorkOrder order : orders) {
            order.setAssignedTo(assigneeId);
            order.setStatus("IN_PROGRESS");
            workOrderRepository.save(order);
            count++;
        }
        log.info("批量分配工单：ids={}, assigneeId={}, 成功数={}", ids, assigneeId, count);
        return count;
    }

    /** 批量关闭工单（仅 RESOLVED 状态可关闭） */
    @Transactional
    public int batchClose(List<Long> ids) {
        List<AnomalyWorkOrder> orders = workOrderRepository.findAllById(ids);
        int count = 0;
        for (AnomalyWorkOrder order : orders) {
            if ("RESOLVED".equals(order.getStatus())) {
                order.setStatus("CLOSED");
                order.setClosedAt(LocalDateTime.now());
                workOrderRepository.save(order);
                count++;
            }
        }
        log.info("批量关闭工单：ids={}, 成功数={}", ids, count);
        return count;
    }

    /** 添加评论 */
    @Transactional
    public CommentVO addComment(Long workOrderId, Long userId, String content) {
        workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("工单", workOrderId));

        AnomalyWorkOrderComment comment = new AnomalyWorkOrderComment();
        comment.setWorkOrderId(workOrderId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment = commentRepository.save(comment);

        Map<Long, String> userNames = buildUserNameMap();
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setUserId(comment.getUserId());
        vo.setUserName(userNames.getOrDefault(userId, "未知用户"));
        vo.setContent(comment.getContent());
        vo.setCreatedAt(comment.getCreatedAt());
        log.info("工单评论：workOrderId={}, userId={}", workOrderId, userId);
        return vo;
    }

    // ════════════════════════════════════════════════
    //  统计
    // ════════════════════════════════════════════════

    /** 工单状态统计 */
    public WorkOrderStatsVO getStats() {
        WorkOrderStatsVO stats = new WorkOrderStatsVO();
        stats.setTotal(workOrderRepository.count());
        stats.setOpen(workOrderRepository.countByStatus("OPEN"));
        stats.setInProgress(workOrderRepository.countByStatus("IN_PROGRESS"));
        stats.setResolved(workOrderRepository.countByStatus("RESOLVED"));
        stats.setClosed(workOrderRepository.countByStatus("CLOSED"));
        return stats;
    }

    // ════════════════════════════════════════════════
    //  SLA 管理
    // ════════════════════════════════════════════════

    /**
     * 检查并升级超过 SLA 时限的工单。
     * LOW → NORMAL, NORMAL → HIGH, HIGH → 保持但添加系统评论。
     * @return 升级的工单数量
     */
    @Transactional
    public int checkAndEscalateSLA() {
        List<AnomalyWorkOrder> activeOrders = workOrderRepository.findByStatusIn(
                List.of("OPEN", "IN_PROGRESS"));

        LocalDateTime now = LocalDateTime.now();
        int escalatedCount = 0;

        for (AnomalyWorkOrder order : activeOrders) {
            int slaHours = getSlaHours(order.getPriority());
            LocalDateTime deadline = order.getCreatedAt().plusHours(slaHours);

            if (now.isAfter(deadline)) {
                order.setSlaBreached(true);
                String oldPriority = order.getPriority();

                switch (oldPriority) {
                    case "LOW" -> {
                        order.setPriority("NORMAL");
                        workOrderRepository.save(order);
                        log.info("工单 #{} SLA超期，优先级 LOW → NORMAL", order.getId());
                        escalatedCount++;
                    }
                    case "NORMAL", "MEDIUM" -> {
                        order.setPriority("HIGH");
                        workOrderRepository.save(order);
                        log.info("工单 #{} SLA超期，优先级 {} → HIGH", order.getId(), oldPriority);
                        escalatedCount++;
                    }
                    case "HIGH" -> {
                        workOrderRepository.save(order);
                        // 添加系统评论提醒
                        AnomalyWorkOrderComment sysComment = new AnomalyWorkOrderComment();
                        sysComment.setWorkOrderId(order.getId());
                        sysComment.setUserId(1L); // 系统用户
                        sysComment.setContent("\u26a0 此工单已超过 SLA 时限，请尽快处理");
                        commentRepository.save(sysComment);
                        log.info("工单 #{} SLA超期（HIGH），已添加系统评论", order.getId());
                        escalatedCount++;
                    }
                    default -> {
                        workOrderRepository.save(order);
                        log.warn("工单 #{} 未知优先级 {}，仅标记 SLA 超期", order.getId(), oldPriority);
                    }
                }
            }
        }

        return escalatedCount;
    }

    /** 根据优先级获取 SLA 时限（小时） */
    private int getSlaHours(String priority) {
        return switch (priority) {
            case "HIGH" -> highPriorityHours;
            case "LOW" -> lowPriorityHours;
            default -> normalPriorityHours; // NORMAL, MEDIUM 等
        };
    }

    /** 获取所有超期且未关闭的工单 */
    public List<AnomalyWorkOrder> getSlaBreachedActiveOrders() {
        return workOrderRepository.findBySlaBreachedTrueAndStatusIn(
                List.of("OPEN", "IN_PROGRESS", "RESOLVED"));
    }

    // ════════════════════════════════════════════════
    //  辅助方法
    // ════════════════════════════════════════════════

    private WorkOrderVO convertToVO(AnomalyWorkOrder order) {
        return convertToVO(order, buildDeptNameMap(), buildMaterialNameMap(), buildUserNameMap());
    }

    private WorkOrderVO convertToVO(AnomalyWorkOrder order, Map<Long, String> deptNames,
                                     Map<Long, String> matNames, Map<Long, String> userNames) {
        WorkOrderVO vo = new WorkOrderVO();
        vo.setId(order.getId());
        vo.setDeptId(order.getDeptId());
        vo.setDeptName(deptNames.getOrDefault(order.getDeptId(), "未知科室"));
        vo.setMaterialId(order.getMaterialId());
        vo.setMaterialName(matNames.getOrDefault(order.getMaterialId(), "未知耗材"));
        vo.setAnomalyType(order.getAnomalyType());
        vo.setDeviationRate(order.getDeviationRate());
        vo.setDescription(order.getDescription());
        vo.setStatus(order.getStatus());
        vo.setPriority(order.getPriority());
        vo.setAssignedTo(order.getAssignedTo());
        vo.setAssignedToName(order.getAssignedTo() != null
                ? userNames.getOrDefault(order.getAssignedTo(), "未知用户") : null);
        vo.setResolution(order.getResolution());
        vo.setCreatedBy(order.getCreatedBy());
        vo.setCreatedByName(order.getCreatedBy() != null
                ? userNames.getOrDefault(order.getCreatedBy(), "未知用户") : null);
        vo.setCreatedAt(order.getCreatedAt());
        vo.setUpdatedAt(order.getUpdatedAt());
        vo.setResolvedAt(order.getResolvedAt());
        vo.setClosedAt(order.getClosedAt());
        vo.setSlaBreached(order.getSlaBreached());
        return vo;
    }

    private Map<Long, String> buildDeptNameMap() {
        return departmentRepository.findAll().stream()
                .collect(Collectors.toMap(Department::getId, Department::getDeptName, (a, b) -> a));
    }

    private Map<Long, String> buildMaterialNameMap() {
        return materialRepository.findAll().stream()
                .collect(Collectors.toMap(Material::getId, Material::getMaterialName, (a, b) -> a));
    }

    private Map<Long, String> buildUserNameMap() {
        return userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, User::getRealName, (a, b) -> a));
    }

    // ════════════════════════════════════════════════
    //  内部 VO 定义
    // ════════════════════════════════════════════════

    @Data
    public static class WorkOrderVO {
        private Long id;
        private Long deptId;
        private String deptName;
        private Long materialId;
        private String materialName;
        private String anomalyType;
        private Double deviationRate;
        private String description;
        private String status;
        private String priority;
        private Long assignedTo;
        private String assignedToName;
        private String resolution;
        private Long createdBy;
        private String createdByName;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime resolvedAt;
        private LocalDateTime closedAt;
        private Boolean slaBreached;
        private List<CommentVO> comments;
    }

    @Data
    public static class CommentVO {
        private Long id;
        private Long userId;
        private String userName;
        private String content;
        private LocalDateTime createdAt;
    }

    @Data
    public static class CreateWorkOrderInput {
        private Long deptId;
        private Long materialId;
        private String anomalyType;
        private Double deviationRate;
        private String description;
        private String priority;
    }

    @Data
    public static class WorkOrderStatsVO {
        private long total;
        private long open;
        private long inProgress;
        private long resolved;
        private long closed;
    }
}
