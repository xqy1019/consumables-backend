package com.medical.system.service.impl;

import com.medical.system.common.PageResult;
import com.medical.system.entity.*;
import com.medical.system.exception.BusinessException;
import com.medical.system.repository.*;
import com.medical.system.security.SecurityUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseServiceImpl {

    private final PurchaseRequisitionRepository reqRepo;
    private final PurchaseRequisitionItemRepository reqItemRepo;
    private final PurchaseInquiryRepository inquiryRepo;
    private final PurchaseInquiryItemRepository inquiryItemRepo;
    private final PurchaseContractRepository contractRepo;
    private final PurchaseContractItemRepository contractItemRepo;
    private final MaterialRepository materialRepo;
    private final DepartmentRepository deptRepo;
    private final SupplierRepository supplierRepo;
    private final UserRepository userRepo;
    private final InventoryRepository inventoryRepo;
    private final InventoryTransactionRepository transactionRepo;

    private String genNo(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    // ==================== 请购单 ====================
    public PageResult<RequisitionVO> getRequisitions(String keyword, String status, Pageable pageable) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword;
        String st = (status == null || status.isBlank()) ? null : status;
        // 科室隔离：非跨科室角色只能查看本科室数据
        Long deptId = SecurityUtils.canAccessAllDepts() ? null : SecurityUtils.getCurrentDeptId();
        Page<PurchaseRequisition> page = reqRepo.findByConditions(kw, st, deptId, pageable);
        return PageResult.of(page.getContent().stream().map(this::toReqVO).collect(Collectors.toList()),
                page.getTotalElements(), pageable.getPageNumber() + 1, pageable.getPageSize());
    }

    @Transactional
    public RequisitionVO createRequisition(PurchaseRequisition req, List<PurchaseRequisitionItem> items) {
        req.setReqNo(genNo("PUR"));
        req.setReqDate(LocalDateTime.now());
        req.setStatus("DRAFT");
        req.setCreatedBy(SecurityUtils.getCurrentUserId());

        BigDecimal total = items.stream()
                .map(i -> i.getEstimatedPrice() != null ? i.getEstimatedPrice().multiply(BigDecimal.valueOf(i.getQuantity())) : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        req.setTotalAmount(total);

        PurchaseRequisition saved = reqRepo.save(req);
        for (PurchaseRequisitionItem item : items) {
            item.setReqId(saved.getId());
            if (item.getEstimatedPrice() != null) {
                item.setSubtotal(item.getEstimatedPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
            reqItemRepo.save(item);
        }
        return toReqVO(saved);
    }

    public RequisitionVO getRequisitionDetail(Long id) {
        PurchaseRequisition req = reqRepo.findById(id).orElseThrow(() -> new BusinessException("请购单不存在"));
        RequisitionVO vo = toReqVO(req);
        vo.setItems(reqItemRepo.findByReqId(id).stream().map(item -> {
            RequisitionItemVO ivo = new RequisitionItemVO();
            ivo.setId(item.getId());
            ivo.setMaterialId(item.getMaterialId());
            ivo.setQuantity(item.getQuantity());
            ivo.setEstimatedPrice(item.getEstimatedPrice());
            ivo.setSubtotal(item.getSubtotal());
            ivo.setRemark(item.getRemark());
            materialRepo.findById(item.getMaterialId()).ifPresent(m -> {
                ivo.setMaterialName(m.getMaterialName());
                ivo.setMaterialCode(m.getMaterialCode());
                ivo.setSpecification(m.getSpecification());
                ivo.setUnit(m.getUnit());
            });
            return ivo;
        }).collect(Collectors.toList()));
        return vo;
    }

    public void submitRequisition(Long id) {
        PurchaseRequisition req = reqRepo.findById(id).orElseThrow(() -> new BusinessException("请购单不存在"));
        if (!"DRAFT".equals(req.getStatus())) throw new BusinessException("只有草稿状态可提交");
        req.setStatus("PENDING");
        reqRepo.save(req);
    }

    public void approveRequisition(Long id, String remark) {
        PurchaseRequisition req = reqRepo.findById(id).orElseThrow(() -> new BusinessException("请购单不存在"));
        if (!"PENDING".equals(req.getStatus())) throw new BusinessException("只有待审批状态可审批");
        req.setStatus("APPROVED");
        req.setApprovedBy(SecurityUtils.getCurrentUserId());
        req.setApprovedTime(LocalDateTime.now());
        req.setApprovalRemark(remark);
        reqRepo.save(req);
    }

    public void rejectRequisition(Long id, String remark) {
        PurchaseRequisition req = reqRepo.findById(id).orElseThrow(() -> new BusinessException("请购单不存在"));
        req.setStatus("REJECTED");
        req.setApprovedBy(SecurityUtils.getCurrentUserId());
        req.setApprovedTime(LocalDateTime.now());
        req.setApprovalRemark(remark);
        reqRepo.save(req);
    }

    // ==================== 询价单 ====================
    public PageResult<InquiryVO> getInquiries(String keyword, String status, Pageable pageable) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword;
        String st = (status == null || status.isBlank()) ? null : status;
        Page<PurchaseInquiry> page = inquiryRepo.findByConditions(kw, st, pageable);
        return PageResult.of(page.getContent().stream().map(this::toInquiryVO).collect(Collectors.toList()),
                page.getTotalElements(), pageable.getPageNumber() + 1, pageable.getPageSize());
    }

    @Transactional
    public InquiryVO createInquiry(PurchaseInquiry inquiry, List<PurchaseInquiryItem> items) {
        inquiry.setInquiryNo(genNo("INQ"));
        inquiry.setInquiryDate(LocalDateTime.now());
        inquiry.setStatus("SENT");
        inquiry.setCreatedBy(SecurityUtils.getCurrentUserId());

        BigDecimal total = items.stream()
                .map(i -> i.getQuotedPrice() != null ? i.getQuotedPrice().multiply(BigDecimal.valueOf(i.getQuantity())) : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        inquiry.setTotalAmount(total);

        PurchaseInquiry saved = inquiryRepo.save(inquiry);
        for (PurchaseInquiryItem item : items) {
            item.setInquiryId(saved.getId());
            if (item.getQuotedPrice() != null) {
                item.setSubtotal(item.getQuotedPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
            inquiryItemRepo.save(item);
        }
        return toInquiryVO(saved);
    }

    public InquiryVO getInquiryDetail(Long id) {
        PurchaseInquiry inquiry = inquiryRepo.findById(id).orElseThrow(() -> new BusinessException("询价单不存在"));
        InquiryVO vo = toInquiryVO(inquiry);
        vo.setItems(inquiryItemRepo.findByInquiryId(id).stream().map(item -> {
            InquiryItemVO ivo = new InquiryItemVO();
            ivo.setId(item.getId());
            ivo.setMaterialId(item.getMaterialId());
            ivo.setQuantity(item.getQuantity());
            ivo.setQuotedPrice(item.getQuotedPrice());
            ivo.setSubtotal(item.getSubtotal());
            ivo.setDeliveryDays(item.getDeliveryDays());
            ivo.setRemark(item.getRemark());
            materialRepo.findById(item.getMaterialId()).ifPresent(m -> {
                ivo.setMaterialName(m.getMaterialName());
                ivo.setSpecification(m.getSpecification());
                ivo.setUnit(m.getUnit());
            });
            return ivo;
        }).collect(Collectors.toList()));
        return vo;
    }

    public void confirmInquiry(Long id) {
        PurchaseInquiry inquiry = inquiryRepo.findById(id).orElseThrow(() -> new BusinessException("询价单不存在"));
        inquiry.setStatus("CONFIRMED");
        inquiryRepo.save(inquiry);
    }

    // ==================== 合同 ====================
    public PageResult<ContractVO> getContracts(String keyword, String status, Pageable pageable) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword;
        String st = (status == null || status.isBlank()) ? null : status;
        Page<PurchaseContract> page = contractRepo.findByConditions(kw, st, pageable);
        return PageResult.of(page.getContent().stream().map(this::toContractVO).collect(Collectors.toList()),
                page.getTotalElements(), pageable.getPageNumber() + 1, pageable.getPageSize());
    }

    @Transactional
    public ContractVO createContract(PurchaseContract contract, List<PurchaseContractItem> items) {
        contract.setContractNo(genNo("CON"));
        contract.setStatus("ACTIVE");
        contract.setCreatedBy(SecurityUtils.getCurrentUserId());

        BigDecimal total = items.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        contract.setTotalAmount(total);

        PurchaseContract saved = contractRepo.save(contract);
        for (PurchaseContractItem item : items) {
            item.setContractId(saved.getId());
            item.setTotalPrice(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            contractItemRepo.save(item);
        }
        return toContractVO(saved);
    }

    public ContractVO getContractDetail(Long id) {
        PurchaseContract contract = contractRepo.findById(id).orElseThrow(() -> new BusinessException("合同不存在"));
        ContractVO vo = toContractVO(contract);
        vo.setItems(contractItemRepo.findByContractId(id).stream().map(item -> {
            ContractItemVO civo = new ContractItemVO();
            civo.setId(item.getId());
            civo.setMaterialId(item.getMaterialId());
            civo.setQuantity(item.getQuantity());
            civo.setUnitPrice(item.getUnitPrice());
            civo.setTotalPrice(item.getTotalPrice());
            civo.setDeliveredQuantity(item.getDeliveredQuantity());
            civo.setRemark(item.getRemark());
            materialRepo.findById(item.getMaterialId()).ifPresent(m -> {
                civo.setMaterialName(m.getMaterialName());
                civo.setSpecification(m.getSpecification());
                civo.setUnit(m.getUnit());
            });
            return civo;
        }).collect(Collectors.toList()));
        return vo;
    }

    public void executeContract(Long id) {
        PurchaseContract contract = contractRepo.findById(id).orElseThrow(() -> new BusinessException("合同不存在"));
        contract.setStatus("EXECUTED");
        contractRepo.save(contract);
    }

    /**
     * 采购收货入库：将请购单中的耗材实际入库，更新库存并记录流水，状态变更为 RECEIVED。
     */
    @Transactional
    public void receiveGoods(Long reqId, List<ReceiveItemVO> items) {
        PurchaseRequisition req = reqRepo.findById(reqId)
                .orElseThrow(() -> new BusinessException("请购单不存在"));
        if (!"APPROVED".equals(req.getStatus())) {
            throw new BusinessException("只有已审批的请购单才能办理收货入库");
        }

        for (ReceiveItemVO item : items) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) continue;

            // 创建库存批次
            Inventory inv = new Inventory();
            inv.setMaterialId(item.getMaterialId());
            inv.setBatchNumber(item.getBatchNumber() != null ? item.getBatchNumber()
                    : "PUR" + reqId + "-" + System.currentTimeMillis());
            inv.setQuantity(item.getQuantity());
            inv.setSupplierId(item.getSupplierId());
            inv.setLocation(item.getLocation());
            inv.setManufactureDate(item.getManufactureDate());
            inv.setExpiryDate(item.getExpiryDate());
            inv.setReceiveDate(LocalDate.now());
            Inventory saved = inventoryRepo.save(inv);

            // 记录入库流水
            InventoryTransaction tx = new InventoryTransaction();
            tx.setMaterialId(item.getMaterialId());
            tx.setTransactionType("INBOUND");
            tx.setQuantity(item.getQuantity());
            tx.setBatchNumber(saved.getBatchNumber());
            tx.setRemark("采购收货 " + req.getReqNo());
            transactionRepo.save(tx);

            log.info("采购收货：请购单={} 耗材ID={} 数量={}", req.getReqNo(), item.getMaterialId(), item.getQuantity());
        }

        req.setStatus("RECEIVED");
        reqRepo.save(req);
    }

    public List<AutoSuggestionVO> getAutoSuggestions() {
        List<Material> materials = materialRepo.findAll().stream()
                .filter(m -> m.getStatus() == 1).collect(Collectors.toList());
        List<AutoSuggestionVO> result = new ArrayList<>();
        for (Material m : materials) {
            Integer totalStock = inventoryRepo.sumQuantityByMaterialId(m.getId());
            int stock = totalStock != null ? totalStock : 0;
            if (m.getMinStock() != null && stock < m.getMinStock()) {
                AutoSuggestionVO vo = new AutoSuggestionVO();
                vo.setMaterialId(m.getId());
                vo.setMaterialName(m.getMaterialName());
                vo.setMaterialCode(m.getMaterialCode());
                vo.setCurrentStock(stock);
                vo.setMinStock(m.getMinStock());
                int maxStock = m.getMaxStock() != null ? m.getMaxStock() : m.getMinStock() * 10;
                vo.setMaxStock(maxStock);
                vo.setSuggestedQuantity(maxStock - stock);
                if (m.getStandardPrice() != null) {
                    vo.setEstimatedCost(m.getStandardPrice().multiply(BigDecimal.valueOf(maxStock - stock)));
                }
                if (m.getSupplierId() != null) {
                    supplierRepo.findById(m.getSupplierId()).ifPresent(s -> vo.setSupplierName(s.getSupplierName()));
                }
                result.add(vo);
            }
        }
        return result;
    }

    private RequisitionVO toReqVO(PurchaseRequisition r) {
        RequisitionVO vo = new RequisitionVO();
        vo.setId(r.getId()); vo.setReqNo(r.getReqNo()); vo.setDeptId(r.getDeptId());
        vo.setReqDate(r.getReqDate()); vo.setRequiredDate(r.getRequiredDate());
        vo.setStatus(r.getStatus()); vo.setTotalAmount(r.getTotalAmount());
        vo.setRemark(r.getRemark()); vo.setApprovedTime(r.getApprovedTime());
        vo.setApprovalRemark(r.getApprovalRemark()); vo.setCreateTime(r.getCreateTime());
        if (r.getDeptId() != null) deptRepo.findById(r.getDeptId()).ifPresent(d -> vo.setDeptName(d.getDeptName()));
        if (r.getCreatedBy() != null) userRepo.findById(r.getCreatedBy()).ifPresent(u -> vo.setCreatedByName(u.getRealName()));
        if (r.getApprovedBy() != null) userRepo.findById(r.getApprovedBy()).ifPresent(u -> vo.setApprovedByName(u.getRealName()));
        return vo;
    }

    private InquiryVO toInquiryVO(PurchaseInquiry i) {
        InquiryVO vo = new InquiryVO();
        vo.setId(i.getId()); vo.setInquiryNo(i.getInquiryNo()); vo.setReqId(i.getReqId());
        vo.setSupplierId(i.getSupplierId()); vo.setInquiryDate(i.getInquiryDate());
        vo.setValidDate(i.getValidDate()); vo.setStatus(i.getStatus());
        vo.setTotalAmount(i.getTotalAmount()); vo.setRemark(i.getRemark()); vo.setCreateTime(i.getCreateTime());
        if (i.getSupplierId() != null) supplierRepo.findById(i.getSupplierId()).ifPresent(s -> vo.setSupplierName(s.getSupplierName()));
        if (i.getReqId() != null) reqRepo.findById(i.getReqId()).ifPresent(r -> vo.setReqNo(r.getReqNo()));
        return vo;
    }

    private ContractVO toContractVO(PurchaseContract c) {
        ContractVO vo = new ContractVO();
        vo.setId(c.getId()); vo.setContractNo(c.getContractNo()); vo.setInquiryId(c.getInquiryId());
        vo.setSupplierId(c.getSupplierId()); vo.setContractDate(c.getContractDate());
        vo.setDeliveryDate(c.getDeliveryDate()); vo.setTotalAmount(c.getTotalAmount());
        vo.setStatus(c.getStatus()); vo.setRemark(c.getRemark()); vo.setCreateTime(c.getCreateTime());
        if (c.getSupplierId() != null) supplierRepo.findById(c.getSupplierId()).ifPresent(s -> vo.setSupplierName(s.getSupplierName()));
        if (c.getInquiryId() != null) inquiryRepo.findById(c.getInquiryId()).ifPresent(inq -> vo.setInquiryNo(inq.getInquiryNo()));
        return vo;
    }

    @Data public static class RequisitionVO {
        private Long id; private String reqNo; private Long deptId; private String deptName;
        private LocalDateTime reqDate; private java.time.LocalDate requiredDate; private String status;
        private BigDecimal totalAmount; private String remark; private String createdByName; private String approvedByName;
        private LocalDateTime approvedTime; private String approvalRemark; private LocalDateTime createTime;
        private List<RequisitionItemVO> items;
    }
    @Data public static class RequisitionItemVO {
        private Long id; private Long materialId; private String materialName; private String materialCode;
        private String specification; private String unit; private Integer quantity;
        private BigDecimal estimatedPrice; private BigDecimal subtotal; private String remark;
    }
    @Data public static class InquiryVO {
        private Long id; private String inquiryNo; private Long reqId; private String reqNo;
        private Long supplierId; private String supplierName; private LocalDateTime inquiryDate;
        private java.time.LocalDate validDate; private String status; private BigDecimal totalAmount;
        private String remark; private LocalDateTime createTime; private List<InquiryItemVO> items;
    }
    @Data public static class InquiryItemVO {
        private Long id; private Long materialId; private String materialName;
        private String specification; private String unit; private Integer quantity;
        private BigDecimal quotedPrice; private BigDecimal subtotal; private Integer deliveryDays; private String remark;
    }
    @Data public static class ContractVO {
        private Long id; private String contractNo; private Long inquiryId; private String inquiryNo;
        private Long supplierId; private String supplierName; private java.time.LocalDate contractDate;
        private java.time.LocalDate deliveryDate; private BigDecimal totalAmount; private String status;
        private String remark; private LocalDateTime createTime; private List<ContractItemVO> items;
    }
    @Data public static class ContractItemVO {
        private Long id; private Long materialId; private String materialName;
        private String specification; private String unit; private Integer quantity;
        private BigDecimal unitPrice; private BigDecimal totalPrice; private Integer deliveredQuantity; private String remark;
    }
    @Data public static class AutoSuggestionVO {
        private Long materialId; private String materialName; private String materialCode; private String supplierName;
        private Integer currentStock; private Integer minStock; private Integer maxStock;
        private Integer suggestedQuantity; private BigDecimal estimatedCost;
    }
    @Data public static class ReceiveItemVO {
        private Long materialId; private Integer quantity; private String batchNumber;
        private Long supplierId; private String location;
        private LocalDate manufactureDate; private LocalDate expiryDate;
    }
}
