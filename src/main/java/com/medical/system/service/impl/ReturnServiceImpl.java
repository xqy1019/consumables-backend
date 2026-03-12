package com.medical.system.service.impl;

import com.medical.system.common.PageResult;
import com.medical.system.entity.*;
import com.medical.system.exception.BusinessException;
import com.medical.system.repository.*;
import com.medical.system.security.SecurityUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReturnServiceImpl {

    private final ReturnRequestRepository returnRequestRepository;
    private final ReturnRequestItemRepository returnItemRepository;
    private final MaterialRepository materialRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    private static final AtomicInteger SEQ = new AtomicInteger(1);

    @Transactional(readOnly = true)
    public PageResult<ReturnVO> getReturnRequests(String status, Long deptId, Pageable pageable) {
        // 科室隔离：非跨科室角色只能查看本科室数据
        if (!SecurityUtils.canAccessAllDepts()) {
            deptId = SecurityUtils.getCurrentDeptId();
        }
        Page<ReturnRequest> page = returnRequestRepository.findByConditions(status, deptId, pageable);
        List<ReturnVO> records = page.getContent().stream()
                .map(this::toVO).collect(Collectors.toList());
        return PageResult.of(records, page.getTotalElements(), pageable.getPageNumber() + 1, pageable.getPageSize());
    }

    @Transactional(readOnly = true)
    public ReturnVO getById(Long id) {
        ReturnRequest req = returnRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException("退料申请不存在"));
        return toVO(req);
    }

    @Transactional
    public ReturnVO createReturn(CreateReturnRequest request, Long userId) {
        checkDeptAccess(request.getDeptId());
        ReturnRequest ret = new ReturnRequest();
        ret.setReturnNo(generateReturnNo());
        ret.setDeptId(request.getDeptId());
        ret.setRemark(request.getRemark());
        ret.setStatus("PENDING");
        ret.setCreatedBy(userId);

        // 先保存父记录获取 ID，再手动保存子记录（避免 Hibernate 单向 @JoinColumn NOT NULL 问题）
        ReturnRequest saved = returnRequestRepository.save(ret);

        if (request.getItems() != null) {
            request.getItems().forEach(i -> {
                ReturnRequestItem item = new ReturnRequestItem();
                item.setReturnId(saved.getId());
                item.setMaterialId(i.getMaterialId());
                item.setBatchNumber(i.getBatchNumber());
                item.setQuantity(i.getQuantity());
                item.setRemark(i.getRemark());
                returnItemRepository.save(item);
            });
        }

        return toVO(saved);
    }

    @Transactional
    public ReturnVO approve(Long id, Long approverId, boolean approved, String remark) {
        ReturnRequest ret = returnRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException("退料申请不存在"));
        checkDeptAccess(ret.getDeptId());
        if (!"PENDING".equals(ret.getStatus())) {
            throw new BusinessException("只有待审批状态可以审批");
        }
        // 不能审批自己提交的退料申请
        if (approverId != null && approverId.equals(ret.getCreatedBy())) {
            throw new BusinessException("不能审批自己提交的退料申请");
        }
        ret.setStatus(approved ? "APPROVED" : "REJECTED");
        ret.setApprovedBy(approverId);
        ret.setApprovedTime(LocalDateTime.now());
        if (remark != null) ret.setRemark(ret.getRemark() != null ? ret.getRemark() + " | 审批意见：" + remark : remark);
        return toVO(returnRequestRepository.save(ret));
    }

    @Transactional
    public ReturnVO complete(Long id, Long operatorId) {
        ReturnRequest ret = returnRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException("退料申请不存在"));
        checkDeptAccess(ret.getDeptId());
        if (!"APPROVED".equals(ret.getStatus())) {
            throw new BusinessException("只有已审批状态可以完成入库");
        }

        // 将退料品入库（找对应批次增加库存，没有则新建）
        List<ReturnRequestItem> items = returnItemRepository.findByReturnId(ret.getId());
        for (ReturnRequestItem item : items) {
            if (item.getBatchNumber() != null) {
                List<Inventory> existing = inventoryRepository
                        .findByMaterialIdAndBatchNumber(item.getMaterialId(), item.getBatchNumber());
                if (!existing.isEmpty()) {
                    Inventory inv = existing.get(0);
                    inv.setQuantity(inv.getQuantity() + item.getQuantity());
                    inv.setStatus(1);
                    inventoryRepository.save(inv);
                } else {
                    createNewInventory(item);
                }
            } else {
                createNewInventory(item);
            }

            // 记录入库流水
            InventoryTransaction tx = new InventoryTransaction();
            tx.setMaterialId(item.getMaterialId());
            tx.setTransactionType("RETURN");
            tx.setQuantity(item.getQuantity());
            tx.setBatchNumber(item.getBatchNumber());
            tx.setDeptId(ret.getDeptId());
            tx.setRemark("退料入库 " + ret.getReturnNo());
            inventoryTransactionRepository.save(tx);
        }

        ret.setStatus("COMPLETED");
        return toVO(returnRequestRepository.save(ret));
    }

    private void createNewInventory(ReturnRequestItem item) {
        Inventory inv = new Inventory();
        inv.setMaterialId(item.getMaterialId());
        inv.setBatchNumber(item.getBatchNumber() != null ? item.getBatchNumber() : "RET" + System.currentTimeMillis());
        inv.setQuantity(item.getQuantity());
        inv.setReceiveDate(LocalDate.now());
        inv.setStatus(1);
        inv.setInspectionStatus("PASSED");
        inventoryRepository.save(inv);
    }

    private ReturnVO toVO(ReturnRequest req) {
        ReturnVO vo = new ReturnVO();
        vo.setId(req.getId());
        vo.setReturnNo(req.getReturnNo());
        vo.setDeptId(req.getDeptId());
        vo.setStatus(req.getStatus());
        vo.setRemark(req.getRemark());
        vo.setCreateTime(req.getCreateTime());
        vo.setApprovedTime(req.getApprovedTime());
        if (req.getDeptId() != null) {
            departmentRepository.findById(req.getDeptId())
                    .ifPresent(d -> vo.setDeptName(d.getDeptName()));
        }
        if (req.getCreatedBy() != null) {
            userRepository.findById(req.getCreatedBy())
                    .ifPresent(u -> vo.setCreatedByName(u.getRealName()));
        }
        if (req.getApprovedBy() != null) {
            userRepository.findById(req.getApprovedBy())
                    .ifPresent(u -> vo.setApprovedByName(u.getRealName()));
        }
        List<ReturnItemVO> items = returnItemRepository.findByReturnId(req.getId()).stream().map(item -> {
            ReturnItemVO iv = new ReturnItemVO();
            iv.setId(item.getId());
            iv.setMaterialId(item.getMaterialId());
            iv.setBatchNumber(item.getBatchNumber());
            iv.setQuantity(item.getQuantity());
            iv.setRemark(item.getRemark());
            materialRepository.findById(item.getMaterialId()).ifPresent(m -> {
                iv.setMaterialName(m.getMaterialName());
                iv.setSpecification(m.getSpecification());
                iv.setUnit(m.getUnit());
            });
            return iv;
        }).collect(Collectors.toList());
        vo.setItems(items);
        return vo;
    }

    /** 非跨科室角色只能操作本科室的退料申请 */
    private void checkDeptAccess(Long resourceDeptId) {
        if (!SecurityUtils.canAccessAllDepts()) {
            if (!Objects.equals(SecurityUtils.getCurrentDeptId(), resourceDeptId)) {
                throw new BusinessException("无权操作其他科室的退料申请");
            }
        }
    }

    private String generateReturnNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "RET" + date + String.format("%04d", SEQ.getAndIncrement() % 10000);
    }

    // ============ VO / Request classes ============

    @Data
    public static class ReturnVO {
        private Long id;
        private String returnNo;
        private Long deptId;
        private String deptName;
        private String status;
        private String remark;
        private String createdByName;
        private String approvedByName;
        private LocalDateTime createTime;
        private LocalDateTime approvedTime;
        private List<ReturnItemVO> items;
    }

    @Data
    public static class ReturnItemVO {
        private Long id;
        private Long materialId;
        private String materialName;
        private String specification;
        private String unit;
        private String batchNumber;
        private Integer quantity;
        private String remark;
    }

    @Data
    public static class CreateReturnRequest {
        private Long deptId;
        private String remark;
        private List<ItemRequest> items;

        @Data
        public static class ItemRequest {
            private Long materialId;
            private String batchNumber;
            private Integer quantity;
            private String remark;
        }
    }

    @Data
    public static class ApproveReturnRequest {
        private boolean approved;
        private String remark;
    }
}
