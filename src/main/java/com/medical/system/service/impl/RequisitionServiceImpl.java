package com.medical.system.service.impl;

import com.medical.system.common.PageResult;
import com.medical.system.dto.request.ApprovalRequest;
import com.medical.system.dto.request.CreateRequisitionRequest;
import com.medical.system.dto.response.RequisitionResponse;
import com.medical.system.entity.*;
import com.medical.system.exception.BusinessException;
import com.medical.system.repository.*;
import com.medical.system.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequisitionServiceImpl {

    private final RequisitionRepository requisitionRepository;
    private final RequisitionItemRepository requisitionItemRepository;
    private final ApprovalRecordRepository approvalRecordRepository;
    private final MaterialRepository materialRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    @Transactional(readOnly = true)
    public PageResult<RequisitionResponse> getRequisitions(String status, Long deptId, Long createdBy, Pageable pageable) {
        // 科室隔离：非跨科室角色只能查看本科室数据
        if (!SecurityUtils.canAccessAllDepts()) {
            deptId = SecurityUtils.getCurrentDeptId();
        }
        Page<Requisition> page = requisitionRepository.findByConditions(status, deptId, createdBy, pageable);
        List<RequisitionResponse> records = page.getContent().stream()
                .map(this::convertToResponse).collect(Collectors.toList());
        return PageResult.of(records, page.getTotalElements(), pageable.getPageNumber() + 1, pageable.getPageSize());
    }

    @Transactional(readOnly = true)
    public RequisitionResponse getRequisitionById(Long id) {
        Requisition requisition = requisitionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("申领单不存在"));
        return convertToResponse(requisition);
    }

    @Transactional
    public RequisitionResponse createRequisition(CreateRequisitionRequest request, Long userId) {
        Requisition requisition = new Requisition();
        requisition.setRequisitionNo(generateRequisitionNo());
        requisition.setDeptId(request.getDeptId());
        requisition.setRequisitionDate(LocalDateTime.now());
        requisition.setRequiredDate(request.getRequiredDate());
        requisition.setRemark(request.getRemark());
        requisition.setStatus("DRAFT");
        requisition.setCreatedBy(userId);

        // 先保存父记录获取 ID，再手动保存子记录（避免 Hibernate 单向 @JoinColumn NOT NULL 问题）
        Requisition saved = requisitionRepository.save(requisition);

        if (request.getItems() != null) {
            request.getItems().forEach(itemReq -> {
                RequisitionItem item = new RequisitionItem();
                item.setRequisitionId(saved.getId());
                item.setMaterialId(itemReq.getMaterialId());
                item.setQuantity(itemReq.getQuantity());
                item.setRemark(itemReq.getRemark());
                requisitionItemRepository.save(item);
            });
        }

        return convertToResponse(saved);
    }

    @Transactional
    public RequisitionResponse submitRequisition(Long id) {
        Requisition requisition = requisitionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("申领单不存在"));
        checkDeptAccess(requisition.getDeptId());
        if (!"DRAFT".equals(requisition.getStatus())) {
            throw new BusinessException("只有草稿状态的申领单可以提交");
        }
        requisition.setStatus("PENDING");
        return convertToResponse(requisitionRepository.save(requisition));
    }

    @Transactional
    public RequisitionResponse approveRequisition(Long id, Long approverId, ApprovalRequest request) {
        Requisition requisition = requisitionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("申领单不存在"));
        checkDeptAccess(requisition.getDeptId());
        if (!"PENDING".equals(requisition.getStatus())) {
            throw new BusinessException("只有待审批状态的申领单可以审批");
        }
        requisition.setStatus("APPROVED");

        ApprovalRecord record = new ApprovalRecord();
        record.setRequisitionId(id);
        record.setApproverId(approverId);
        record.setApprovalTime(LocalDateTime.now());
        record.setStatus("APPROVED");
        record.setRemark(request.getRemark());
        approvalRecordRepository.save(record);

        return convertToResponse(requisitionRepository.save(requisition));
    }

    @Transactional
    public RequisitionResponse rejectRequisition(Long id, Long approverId, ApprovalRequest request) {
        Requisition requisition = requisitionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("申领单不存在"));
        checkDeptAccess(requisition.getDeptId());
        if (!"PENDING".equals(requisition.getStatus())) {
            throw new BusinessException("只有待审批状态的申领单可以驳回");
        }
        requisition.setStatus("REJECTED");

        ApprovalRecord record = new ApprovalRecord();
        record.setRequisitionId(id);
        record.setApproverId(approverId);
        record.setApprovalTime(LocalDateTime.now());
        record.setStatus("REJECTED");
        record.setRemark(request.getRemark());
        approvalRecordRepository.save(record);

        return convertToResponse(requisitionRepository.save(requisition));
    }

    @Transactional
    public RequisitionResponse dispatchRequisition(Long id) {
        Requisition requisition = requisitionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("申领单不存在"));
        // 非跨科室角色（如库管员）只能发放本科室的申领单
        checkDeptAccess(requisition.getDeptId());
        if (!"APPROVED".equals(requisition.getStatus())) {
            throw new BusinessException("只有已审批状态的申领单可以发放");
        }

        // 按 FEFO（先进先出按效期）自动扣减库存
        List<RequisitionItem> reqItems = requisitionItemRepository.findByRequisitionId(id);
        for (RequisitionItem item : reqItems) {
            int remaining = item.getQuantity();
            // 使用悲观写锁查询可用批次，防止并发发放超卖
            List<Inventory> batches = inventoryRepository.findAvailableByMaterialIdFEFOForUpdate(item.getMaterialId());

            int totalAvailable = batches.stream().mapToInt(Inventory::getQuantity).sum();
            if (totalAvailable < remaining) {
                String matName = materialRepository.findById(item.getMaterialId())
                        .map(Material::getMaterialName).orElse("ID:" + item.getMaterialId());
                throw new BusinessException("耗材【" + matName + "】库存不足，可用:" + totalAvailable + "，需要:" + remaining);
            }

            int actualOutbound = 0;
            for (Inventory batch : batches) {
                if (remaining <= 0) break;
                int deduct = Math.min(batch.getQuantity(), remaining);
                batch.setQuantity(batch.getQuantity() - deduct);
                if (batch.getQuantity() == 0) batch.setStatus(0);
                inventoryRepository.save(batch);

                // 记录出库流水
                InventoryTransaction tx = new InventoryTransaction();
                tx.setMaterialId(item.getMaterialId());
                tx.setTransactionType("OUTBOUND");
                tx.setQuantity(deduct);
                tx.setBatchNumber(batch.getBatchNumber());
                tx.setDeptId(requisition.getDeptId());
                tx.setRequisitionId(id);
                tx.setRemark("申领发放 " + requisition.getRequisitionNo());
                inventoryTransactionRepository.save(tx);

                remaining -= deduct;
                actualOutbound += deduct;
            }
            // 记录实际发放数量
            item.setActualQuantity(actualOutbound);
            requisitionItemRepository.save(item);
            log.info("申领发放：耗材ID={} 申请{}件，实际出库{}件", item.getMaterialId(), item.getQuantity(), actualOutbound);
        }

        requisition.setStatus("DISPATCHED");
        return convertToResponse(requisitionRepository.save(requisition));
    }

    @Transactional
    public RequisitionResponse signRequisition(Long id, Long signerId, String signRemark) {
        Requisition requisition = requisitionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("申领单不存在"));
        checkDeptAccess(requisition.getDeptId());
        if (!"DISPATCHED".equals(requisition.getStatus())) {
            throw new BusinessException("只有已发放状态的申领单可以签收");
        }
        requisition.setStatus("SIGNED");
        requisition.setSignedBy(signerId);
        requisition.setSignTime(LocalDateTime.now());
        requisition.setSignRemark(signRemark);
        return convertToResponse(requisitionRepository.save(requisition));
    }

    /**
     * 校验当前用户是否有权操作指定科室的单据。
     * 非跨科室角色（如护士长）只能操作本科室的单据。
     */
    private void checkDeptAccess(Long resourceDeptId) {
        if (!SecurityUtils.canAccessAllDepts()) {
            if (!Objects.equals(SecurityUtils.getCurrentDeptId(), resourceDeptId)) {
                throw new BusinessException("无权操作其他科室的单据");
            }
        }
    }

    private String generateRequisitionNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = requisitionRepository.nextRequisitionSeq();
        return "REQ" + date + String.format("%06d", seq);
    }

    private String getStatusLabel(String status) {
        return switch (status) {
            case "DRAFT" -> "草稿";
            case "PENDING" -> "待审批";
            case "APPROVED" -> "已审批";
            case "REJECTED" -> "已驳回";
            case "DISPATCHED" -> "已发放";
            case "SIGNED" -> "已签收";
            default -> status;
        };
    }

    public RequisitionResponse convertToResponse(Requisition req) {
        RequisitionResponse response = new RequisitionResponse();
        response.setId(req.getId());
        response.setRequisitionNo(req.getRequisitionNo());
        response.setDeptId(req.getDeptId());
        response.setRequisitionDate(req.getRequisitionDate());
        response.setRequiredDate(req.getRequiredDate());
        response.setStatus(req.getStatus());
        response.setStatusLabel(getStatusLabel(req.getStatus()));
        response.setRemark(req.getRemark());
        response.setCreatedBy(req.getCreatedBy());
        response.setCreateTime(req.getCreateTime());

        departmentRepository.findById(req.getDeptId())
                .ifPresent(d -> response.setDeptName(d.getDeptName()));

        if (req.getCreatedBy() != null) {
            userRepository.findById(req.getCreatedBy())
                    .ifPresent(u -> response.setCreatedByName(u.getRealName()));
        }

        response.setSignedBy(req.getSignedBy());
        response.setSignTime(req.getSignTime());
        response.setSignRemark(req.getSignRemark());
        if (req.getSignedBy() != null) {
            userRepository.findById(req.getSignedBy())
                    .ifPresent(u -> response.setSignedByName(u.getRealName()));
        }

        List<RequisitionResponse.ItemResponse> itemResponses = requisitionItemRepository.findByRequisitionId(req.getId()).stream().map(item -> {
            RequisitionResponse.ItemResponse ir = new RequisitionResponse.ItemResponse();
            ir.setId(item.getId());
            ir.setMaterialId(item.getMaterialId());
            ir.setQuantity(item.getQuantity());
            ir.setActualQuantity(item.getActualQuantity());
            ir.setRemark(item.getRemark());
            materialRepository.findById(item.getMaterialId()).ifPresent(m -> {
                ir.setMaterialName(m.getMaterialName());
                ir.setSpecification(m.getSpecification());
                ir.setUnit(m.getUnit());
            });
            return ir;
        }).collect(Collectors.toList());
        response.setItems(itemResponses);

        List<ApprovalRecord> approvalRecords = approvalRecordRepository
                .findByRequisitionIdOrderByApprovalTimeAsc(req.getId());
        List<RequisitionResponse.ApprovalRecordResponse> approvalResponses = approvalRecords.stream().map(ar -> {
            RequisitionResponse.ApprovalRecordResponse arr = new RequisitionResponse.ApprovalRecordResponse();
            arr.setId(ar.getId());
            arr.setApproverId(ar.getApproverId());
            arr.setApprovalTime(ar.getApprovalTime());
            arr.setStatus(ar.getStatus());
            arr.setRemark(ar.getRemark());
            if (ar.getApproverId() != null) {
                userRepository.findById(ar.getApproverId())
                        .ifPresent(u -> arr.setApproverName(u.getRealName()));
            }
            return arr;
        }).collect(Collectors.toList());
        response.setApprovalRecords(approvalResponses);

        return response;
    }
}
