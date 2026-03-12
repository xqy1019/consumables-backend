package com.medical.system.service.impl;

import com.medical.system.common.PageResult;
import com.medical.system.entity.*;
import com.medical.system.exception.BusinessException;
import com.medical.system.repository.*;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecallServiceImpl {

    private final RecallNoticeRepository recallNoticeRepository;
    private final RecallNoticeBatchRepository batchRepository;
    private final RecallDisposalRepository disposalRepository;
    private final MaterialRepository materialRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;

    private static final AtomicInteger SEQ = new AtomicInteger(1);

    @Transactional(readOnly = true)
    public PageResult<RecallVO> getRecalls(String status, String keyword, Pageable pageable) {
        Page<RecallNotice> page = recallNoticeRepository.findByConditions(status, keyword, pageable);
        List<RecallVO> records = page.getContent().stream()
                .map(this::toVO).collect(Collectors.toList());
        return PageResult.of(records, page.getTotalElements(), pageable.getPageNumber() + 1, pageable.getPageSize());
    }

    @Transactional(readOnly = true)
    public RecallDetailVO getDetail(Long id) {
        RecallNotice notice = recallNoticeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("召回通知不存在"));
        RecallDetailVO vo = new RecallDetailVO();
        vo.setBasic(toVO(notice));

        // 受影响库存
        List<RecallNoticeBatch> batches = batchRepository.findByRecallId(id);
        List<AffectedInventoryVO> affected = batches.stream().flatMap(b -> {
            List<Inventory> invList;
            if (b.getBatchNumber() != null) {
                invList = inventoryRepository.findByMaterialIdAndBatchNumber(b.getMaterialId(), b.getBatchNumber());
            } else {
                invList = inventoryRepository.findByMaterialIdAndStatusNot(b.getMaterialId(), 0);
            }
            return invList.stream().map(inv -> {
                AffectedInventoryVO aiv = new AffectedInventoryVO();
                aiv.setInventoryId(inv.getId());
                aiv.setMaterialId(inv.getMaterialId());
                aiv.setBatchNumber(inv.getBatchNumber());
                aiv.setQuantity(inv.getQuantity());
                aiv.setLocation(inv.getLocation());
                aiv.setExpiryDate(inv.getExpiryDate());
                materialRepository.findById(inv.getMaterialId())
                        .ifPresent(m -> aiv.setMaterialName(m.getMaterialName()));
                return aiv;
            });
        }).collect(Collectors.toList());
        vo.setAffectedInventory(affected);

        // 处置记录
        List<RecallDisposal> disposals = disposalRepository.findByRecallId(id);
        List<DisposalVO> disposalVOs = disposals.stream().map(d -> {
            DisposalVO dv = new DisposalVO();
            dv.setId(d.getId());
            dv.setMaterialId(d.getMaterialId());
            dv.setBatchNumber(d.getBatchNumber());
            dv.setQuantity(d.getQuantity());
            dv.setDisposalType(d.getDisposalType());
            dv.setDisposalDate(d.getDisposalDate());
            dv.setRemark(d.getRemark());
            materialRepository.findById(d.getMaterialId())
                    .ifPresent(m -> dv.setMaterialName(m.getMaterialName()));
            if (d.getOperatorId() != null) {
                userRepository.findById(d.getOperatorId())
                        .ifPresent(u -> dv.setOperatorName(u.getRealName()));
            }
            return dv;
        }).collect(Collectors.toList());
        vo.setDisposals(disposalVOs);

        return vo;
    }

    @Transactional
    public RecallVO createRecall(CreateRecallRequest request, Long userId) {
        RecallNotice notice = new RecallNotice();
        notice.setRecallNo(generateRecallNo());
        notice.setTitle(request.getTitle());
        notice.setRecallReason(request.getRecallReason());
        notice.setRecallLevel(request.getRecallLevel() != null ? request.getRecallLevel() : "II");
        notice.setSource(request.getSource() != null ? request.getSource() : "SUPPLIER");
        notice.setIssuedDate(request.getIssuedDate() != null ? request.getIssuedDate() : LocalDate.now());
        notice.setRemark(request.getRemark());
        notice.setCreatedBy(userId);

        // 先保存父记录获取 ID，再手动保存子记录（避免 Hibernate 单向 @JoinColumn NOT NULL 问题）
        RecallNotice saved = recallNoticeRepository.save(notice);

        if (request.getBatches() != null) {
            request.getBatches().forEach(b -> {
                RecallNoticeBatch batch = new RecallNoticeBatch();
                batch.setRecallId(saved.getId());
                batch.setMaterialId(b.getMaterialId());
                batch.setBatchNumber(b.getBatchNumber());
                batch.setQuantityAffected(b.getQuantityAffected());
                batch.setRemark(b.getRemark());
                batchRepository.save(batch);
            });
        }

        return toVO(saved);
    }

    @Transactional
    public void closeRecall(Long id) {
        RecallNotice notice = recallNoticeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("召回通知不存在"));
        notice.setStatus("CLOSED");
        recallNoticeRepository.save(notice);
    }

    @Transactional
    public DisposalVO addDisposal(Long recallId, AddDisposalRequest request, Long operatorId) {
        recallNoticeRepository.findById(recallId)
                .orElseThrow(() -> new BusinessException("召回通知不存在"));

        RecallDisposal disposal = new RecallDisposal();
        disposal.setRecallId(recallId);
        disposal.setMaterialId(request.getMaterialId());
        disposal.setInventoryId(request.getInventoryId());
        disposal.setBatchNumber(request.getBatchNumber());
        disposal.setQuantity(request.getQuantity());
        disposal.setDisposalType(request.getDisposalType());
        disposal.setDisposalDate(LocalDateTime.now());
        disposal.setRemark(request.getRemark());
        disposal.setOperatorId(operatorId);

        // 如果是销毁或退货，从库存扣减
        if (request.getInventoryId() != null &&
                ("DESTROY".equals(request.getDisposalType()) || "RETURN".equals(request.getDisposalType()))) {
            inventoryRepository.findById(request.getInventoryId()).ifPresent(inv -> {
                int newQty = Math.max(0, inv.getQuantity() - request.getQuantity());
                inv.setQuantity(newQty);
                if (newQty == 0) inv.setStatus(0);
                inventoryRepository.save(inv);
            });
        }

        RecallDisposal saved = disposalRepository.save(disposal);
        DisposalVO dv = new DisposalVO();
        dv.setId(saved.getId());
        dv.setMaterialId(saved.getMaterialId());
        dv.setBatchNumber(saved.getBatchNumber());
        dv.setQuantity(saved.getQuantity());
        dv.setDisposalType(saved.getDisposalType());
        dv.setDisposalDate(saved.getDisposalDate());
        dv.setRemark(saved.getRemark());
        materialRepository.findById(saved.getMaterialId())
                .ifPresent(m -> dv.setMaterialName(m.getMaterialName()));
        userRepository.findById(operatorId).ifPresent(u -> dv.setOperatorName(u.getRealName()));
        return dv;
    }

    private RecallVO toVO(RecallNotice n) {
        RecallVO vo = new RecallVO();
        vo.setId(n.getId());
        vo.setRecallNo(n.getRecallNo());
        vo.setTitle(n.getTitle());
        vo.setRecallReason(n.getRecallReason());
        vo.setRecallLevel(n.getRecallLevel());
        vo.setSource(n.getSource());
        vo.setIssuedDate(n.getIssuedDate());
        vo.setStatus(n.getStatus());
        vo.setRemark(n.getRemark());
        vo.setCreateTime(n.getCreateTime());
        vo.setBatchCount(n.getId() != null ? (int) batchRepository.findByRecallId(n.getId()).size() : 0);
        if (n.getCreatedBy() != null) {
            userRepository.findById(n.getCreatedBy())
                    .ifPresent(u -> vo.setCreatedByName(u.getRealName()));
        }
        return vo;
    }

    private String generateRecallNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "RC" + date + String.format("%04d", SEQ.getAndIncrement() % 10000);
    }

    // ============ VO / Request classes ============

    @Data
    public static class RecallVO {
        private Long id;
        private String recallNo;
        private String title;
        private String recallReason;
        private String recallLevel;
        private String source;
        private LocalDate issuedDate;
        private String status;
        private String remark;
        private int batchCount;
        private String createdByName;
        private LocalDateTime createTime;
    }

    @Data
    public static class RecallDetailVO {
        private RecallVO basic;
        private List<AffectedInventoryVO> affectedInventory;
        private List<DisposalVO> disposals;
    }

    @Data
    public static class AffectedInventoryVO {
        private Long inventoryId;
        private Long materialId;
        private String materialName;
        private String batchNumber;
        private Integer quantity;
        private String location;
        private LocalDate expiryDate;
    }

    @Data
    public static class DisposalVO {
        private Long id;
        private Long materialId;
        private String materialName;
        private String batchNumber;
        private Integer quantity;
        private String disposalType;
        private LocalDateTime disposalDate;
        private String remark;
        private String operatorName;
    }

    @Data
    public static class CreateRecallRequest {
        private String title;
        private String recallReason;
        private String recallLevel;
        private String source;
        private LocalDate issuedDate;
        private String remark;
        private List<BatchItem> batches;

        @Data
        public static class BatchItem {
            private Long materialId;
            private String batchNumber;
            private Integer quantityAffected;
            private String remark;
        }
    }

    @Data
    public static class AddDisposalRequest {
        private Long materialId;
        private Long inventoryId;
        private String batchNumber;
        private Integer quantity;
        private String disposalType;
        private String remark;
    }
}
