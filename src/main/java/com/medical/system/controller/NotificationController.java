package com.medical.system.controller;

import com.medical.system.common.Result;
import com.medical.system.entity.*;
import com.medical.system.repository.*;
import com.medical.system.service.impl.AnomalyWorkOrderService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final RequisitionRepository requisitionRepository;
    private final PurchaseRequisitionRepository purchaseRequisitionRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryBorrowingRepository borrowingRepository;
    private final MaterialRepository materialRepository;
    private final SupplierRepository supplierRepository;
    private final RecallNoticeRepository recallNoticeRepository;
    private final AiExpiryDisposalCacheRepository aiExpiryDisposalCacheRepository;
    private final DeptParLevelRepository deptParLevelRepository;
    private final DeptInventoryRepository deptInventoryRepository;
    private final DepartmentRepository departmentRepository;
    private final AnomalyWorkOrderService anomalyWorkOrderService;

    @GetMapping
    public Result<NotificationResult> getNotifications() {
        List<NotificationVO> items = new ArrayList<>();

        // 1. 待审批的申领单
        List<Requisition> pendingReqs = requisitionRepository.findByStatus("PENDING");
        for (Requisition r : pendingReqs) {
            NotificationVO n = new NotificationVO();
            n.setId("req-" + r.getId());
            n.setType("PENDING_REQUISITION");
            n.setLevel("warning");
            n.setTitle("申领单待审批");
            n.setContent("申领单 " + r.getRequisitionNo() + " 等待审批");
            n.setLinkPath("/requisitions");
            n.setCreateTime(r.getCreateTime());
            items.add(n);
        }

        // 1b. 待签收的申领单（已发放待科室确认）
        List<Requisition> dispatchedReqs = requisitionRepository.findByStatus("DISPATCHED");
        for (Requisition r : dispatchedReqs) {
            NotificationVO n = new NotificationVO();
            n.setId("sign-" + r.getId());
            n.setType("PENDING_SIGN");
            n.setLevel("info");
            n.setTitle("申领单待签收");
            n.setContent("申领单 " + r.getRequisitionNo() + " 已发放，请及时签收确认");
            n.setLinkPath("/requisitions");
            n.setCreateTime(r.getUpdateTime() != null ? r.getUpdateTime() : r.getCreateTime());
            items.add(n);
        }

        // 2. 待审批的采购请购单
        List<PurchaseRequisition> pendingPurch = purchaseRequisitionRepository.findByStatus("PENDING");
        for (PurchaseRequisition r : pendingPurch) {
            NotificationVO n = new NotificationVO();
            n.setId("pur-" + r.getId());
            n.setType("PENDING_PURCHASE");
            n.setLevel("warning");
            n.setTitle("采购请购单待审批");
            n.setContent("请购单 " + r.getReqNo() + " 等待审批");
            n.setLinkPath("/purchase/requisitions");
            n.setCreateTime(r.getCreateTime());
            items.add(n);
        }

        // 3. 7天内即将过期的库存
        LocalDate in7Days = LocalDate.now().plusDays(7);
        List<Inventory> expiring7 = inventoryRepository.findExpiringInventory(in7Days);
        for (Inventory inv : expiring7) {
            materialRepository.findById(inv.getMaterialId()).ifPresent(m -> {
                NotificationVO n = new NotificationVO();
                n.setId("exp-" + inv.getId());
                n.setType("EXPIRY_SOON");
                n.setLevel("error");
                n.setTitle("耗材即将过期");
                String content = m.getMaterialName() + " 批号" + inv.getBatchNumber()
                        + " 将于 " + inv.getExpiryDate() + " 过期";
                // 附加 AI 处置建议
                try {
                    aiExpiryDisposalCacheRepository.findByInventoryId(inv.getId()).ifPresent(cache -> {
                        String adviceLabel = switch (cache.getAction()) {
                            case "ACCELERATE" -> "加速使用";
                            case "TRANSFER" -> "跨科调拨";
                            case "RETURN" -> "联系退货";
                            case "DAMAGE" -> "办理报损";
                            default -> cache.getAction();
                        };
                        n.setContent(content + " 【AI建议：" + adviceLabel + "】");
                    });
                } catch (Exception e) {
                    log.debug("查询AI处置建议缓存失败: {}", e.getMessage());
                }
                if (n.getContent() == null) {
                    n.setContent(content);
                }
                n.setLinkPath("/inventory");
                n.setCreateTime(LocalDateTime.now());
                items.add(n);
            });
        }

        // 4. 逾期未归还的借用单
        List<InventoryBorrowing> overdue = borrowingRepository.findByStatus("OVERDUE");
        for (InventoryBorrowing b : overdue) {
            NotificationVO n = new NotificationVO();
            n.setId("borrow-" + b.getId());
            n.setType("OVERDUE_BORROW");
            n.setLevel("error");
            n.setTitle("借用逾期未归还");
            n.setContent("借用单 " + b.getBorrowingNo() + " 已逾期，请及时跟进");
            n.setLinkPath("/inventory/borrowing");
            n.setCreateTime(b.getCreateTime());
            items.add(n);
        }

        // 5. 注册证即将到期的耗材（60天内）
        LocalDate in60Days = LocalDate.now().plusDays(60);
        materialRepository.findAll().stream()
                .filter(m -> m.getStatus() == 1
                        && m.getRegistrationExpiry() != null
                        && !m.getRegistrationExpiry().isAfter(in60Days))
                .forEach(m -> {
                    NotificationVO n = new NotificationVO();
                    n.setId("reg-" + m.getId());
                    n.setType("CERT_EXPIRY");
                    boolean expired = m.getRegistrationExpiry().isBefore(LocalDate.now());
                    n.setLevel(expired ? "error" : "warning");
                    n.setTitle(expired ? "注册证已过期" : "注册证即将到期");
                    n.setContent(m.getMaterialName() + " 注册证号 " + m.getRegistrationNo()
                            + (expired ? " 已于 " : " 将于 ") + m.getRegistrationExpiry() + (expired ? " 过期" : " 到期"));
                    n.setLinkPath("/materials");
                    n.setCreateTime(LocalDateTime.now());
                    items.add(n);
                });

        // 6. 供应商经营许可证即将到期（60天内）
        supplierRepository.findAll().stream()
                .filter(s -> s.getStatus() == 1
                        && s.getLicenseExpiry() != null
                        && !s.getLicenseExpiry().isAfter(in60Days))
                .forEach(s -> {
                    NotificationVO n = new NotificationVO();
                    n.setId("lic-" + s.getId());
                    n.setType("LICENSE_EXPIRY");
                    boolean expired = s.getLicenseExpiry().isBefore(LocalDate.now());
                    n.setLevel(expired ? "error" : "warning");
                    n.setTitle(expired ? "经营许可证已过期" : "经营许可证即将到期");
                    n.setContent(s.getSupplierName() + " 许可证 " + s.getLicenseNo()
                            + (expired ? " 已于 " : " 将于 ") + s.getLicenseExpiry() + (expired ? " 过期" : " 到期"));
                    n.setLinkPath("/system/suppliers");
                    n.setCreateTime(LocalDateTime.now());
                    items.add(n);
                });

        // 7. 进行中的召回通知
        recallNoticeRepository.findByStatus("ACTIVE").forEach(r -> {
            NotificationVO n = new NotificationVO();
            n.setId("recall-" + r.getId());
            n.setType("ACTIVE_RECALL");
            n.setLevel("error");
            n.setTitle("耗材召回通知");
            n.setContent(r.getTitle() + "（" + r.getRecallNo() + "）正在处理中");
            n.setLinkPath("/recall");
            n.setCreateTime(r.getCreateTime());
            items.add(n);
        });

        // 8. 二级库低库存预警（科室库存低于最低线）
        List<DeptParLevel> activeParLevels = deptParLevelRepository.findByIsActiveTrue();
        for (DeptParLevel par : activeParLevels) {
            deptInventoryRepository.findByDeptIdAndMaterialId(par.getDeptId(), par.getMaterialId())
                    .ifPresent(inv -> {
                        if (par.getMinQuantity() != null
                                && inv.getCurrentQuantity().compareTo(par.getMinQuantity()) < 0) {
                            String deptName = departmentRepository.findById(par.getDeptId())
                                    .map(Department::getDeptName).orElse("未知科室");
                            String matName = materialRepository.findById(par.getMaterialId())
                                    .map(Material::getMaterialName).orElse("未知耗材");
                            NotificationVO n = new NotificationVO();
                            n.setId("low-dept-" + inv.getId());
                            n.setType("LOW_DEPT_STOCK");
                            n.setLevel("warning");
                            n.setTitle(deptName + " - " + matName + " 库存不足");
                            n.setContent("当前库存 " + inv.getCurrentQuantity()
                                    + "，低于最低线 " + par.getMinQuantity() + "，建议立即补货");
                            n.setLinkPath("/consumables/dept-inventory");
                            n.setCreateTime(inv.getUpdatedAt() != null ? inv.getUpdatedAt() : LocalDateTime.now());
                            items.add(n);
                        }
                    });
        }

        // 9. 工单 SLA 超期通知
        try {
            List<AnomalyWorkOrder> slaBreached = anomalyWorkOrderService.getSlaBreachedActiveOrders();
            for (AnomalyWorkOrder wo : slaBreached) {
                NotificationVO n = new NotificationVO();
                n.setId("sla-" + wo.getId());
                n.setType("SLA_BREACH");
                n.setLevel("error");
                n.setTitle("工单 SLA 超期");
                n.setContent("工单 #" + wo.getId() + " 已超过 SLA 时限，当前优先级：" + wo.getPriority());
                n.setLinkPath("/consumables/work-orders");
                n.setCreateTime(wo.getUpdatedAt());
                items.add(n);
            }
        } catch (Exception e) {
            log.debug("查询 SLA 超期工单失败: {}", e.getMessage());
        }

        // 按创建时间倒序
        items.sort((a, b) -> {
            if (a.getCreateTime() == null) return 1;
            if (b.getCreateTime() == null) return -1;
            return b.getCreateTime().compareTo(a.getCreateTime());
        });

        NotificationResult result = new NotificationResult();
        result.setItems(items);
        result.setTotal(items.size());
        result.setUnread(items.size()); // 简化：全部视为未读
        return Result.success(result);
    }

    @Data
    public static class NotificationVO {
        private String id;
        private String type;
        private String level;   // info / warning / error
        private String title;
        private String content;
        private String linkPath;
        private LocalDateTime createTime;
    }

    @Data
    public static class NotificationResult {
        private List<NotificationVO> items;
        private int total;
        private int unread;
    }
}
