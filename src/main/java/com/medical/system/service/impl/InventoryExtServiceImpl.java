package com.medical.system.service.impl;

import com.medical.system.common.PageResult;
import com.medical.system.entity.*;
import com.medical.system.exception.BusinessException;
import com.medical.system.repository.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryExtServiceImpl {

    private final InventoryStocktakingRepository stocktakingRepo;
    private final InventoryStocktakingItemRepository stocktakingItemRepo;
    private final InventoryTransferRepository transferRepo;
    private final InventoryDamageRepository damageRepo;
    private final InventoryBorrowingRepository borrowingRepo;
    private final InventoryRepository inventoryRepo;
    private final MaterialRepository materialRepo;
    private final DepartmentRepository deptRepo;
    private final UserRepository userRepo;
    private final InventoryTransactionRepository transactionRepo;

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails ud) {
            return userRepo.findByUsername(ud.getUsername()).map(User::getId).orElse(null);
        }
        return null;
    }

    private String genNo(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + (System.currentTimeMillis() % 1000);
    }

    // ==================== 盘点 ====================
    public PageResult<StocktakingVO> getStocktakingList(String keyword, String status, Pageable pageable) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword;
        String st = (status == null || status.isBlank()) ? null : status;
        Page<InventoryStocktaking> page = stocktakingRepo.findByConditions(kw, st, pageable);
        List<StocktakingVO> vos = page.getContent().stream().map(this::toStocktakingVO).collect(Collectors.toList());
        return PageResult.of(vos, page.getTotalElements(), pageable.getPageNumber() + 1, pageable.getPageSize());
    }

    @Transactional
    public StocktakingVO createStocktaking(String location, String remark) {
        InventoryStocktaking s = new InventoryStocktaking();
        s.setStocktakingNo(genNo("STCK"));
        s.setStocktakingDate(LocalDateTime.now());
        s.setLocation(location);
        s.setStatus("IN_PROGRESS");
        s.setRemark(remark);
        s.setCreatedBy(getCurrentUserId());
        InventoryStocktaking saved = stocktakingRepo.save(s);

        // 自动加载库存作为盘点明细
        List<Inventory> inventories = inventoryRepo.findAll().stream()
                .filter(inv -> inv.getStatus() == 1)
                .filter(inv -> location == null || location.isBlank() || (inv.getLocation() != null && inv.getLocation().startsWith(location.replace("全库", ""))))
                .collect(Collectors.toList());

        for (Inventory inv : inventories) {
            InventoryStocktakingItem item = new InventoryStocktakingItem();
            item.setStocktakingId(saved.getId());
            item.setMaterialId(inv.getMaterialId());
            item.setInventoryId(inv.getId());
            item.setBatchNumber(inv.getBatchNumber());
            item.setSystemQuantity(inv.getQuantity());
            stocktakingItemRepo.save(item);
        }
        return toStocktakingVO(saved);
    }

    public StocktakingVO getStocktakingDetail(Long id) {
        InventoryStocktaking s = stocktakingRepo.findById(id)
                .orElseThrow(() -> new BusinessException("盘点单不存在"));
        StocktakingVO vo = toStocktakingVO(s);
        List<InventoryStocktakingItem> items = stocktakingItemRepo.findByStocktakingId(id);
        vo.setItems(items.stream().map(item -> {
            StocktakingItemVO ivo = new StocktakingItemVO();
            ivo.setId(item.getId());
            ivo.setMaterialId(item.getMaterialId());
            ivo.setInventoryId(item.getInventoryId());
            ivo.setBatchNumber(item.getBatchNumber());
            ivo.setSystemQuantity(item.getSystemQuantity());
            ivo.setActualQuantity(item.getActualQuantity());
            ivo.setDifference(item.getDifference());
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

    @Transactional
    public void completeStocktaking(Long id, List<Map<String, Object>> itemUpdates) {
        InventoryStocktaking s = stocktakingRepo.findById(id)
                .orElseThrow(() -> new BusinessException("盘点单不存在"));
        if ("COMPLETED".equals(s.getStatus())) {
            throw new BusinessException("盘点单已完成");
        }

        for (Map<String, Object> update : itemUpdates) {
            Long itemId = Long.valueOf(update.get("id").toString());
            Integer actualQty = Integer.valueOf(update.get("actualQuantity").toString());
            stocktakingItemRepo.findById(itemId).ifPresent(item -> {
                item.setActualQuantity(actualQty);
                item.setDifference(actualQty - item.getSystemQuantity());
                if (update.containsKey("remark")) item.setRemark(update.get("remark").toString());
                stocktakingItemRepo.save(item);
                // 同步库存数量
                if (item.getInventoryId() != null) {
                    inventoryRepo.findById(item.getInventoryId()).ifPresent(inv -> {
                        inv.setQuantity(actualQty);
                        if (actualQty == 0) inv.setStatus(0);
                        inventoryRepo.save(inv);
                    });
                }
            });
        }
        s.setStatus("COMPLETED");
        stocktakingRepo.save(s);
    }

    // ==================== 移库 ====================
    public PageResult<TransferVO> getTransferList(String keyword, Pageable pageable) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword;
        Page<InventoryTransfer> page = transferRepo.findByConditions(kw, pageable);
        return PageResult.of(page.getContent().stream().map(this::toTransferVO).collect(Collectors.toList()),
                page.getTotalElements(), pageable.getPageNumber() + 1, pageable.getPageSize());
    }

    @Transactional
    public TransferVO createTransfer(Long inventoryId, Integer quantity, String toLocation, String remark) {
        Inventory inv = inventoryRepo.findById(inventoryId)
                .orElseThrow(() -> new BusinessException("库存记录不存在"));
        if (inv.getQuantity() < quantity) throw new BusinessException("库存不足");

        InventoryTransfer transfer = new InventoryTransfer();
        transfer.setTransferNo(genNo("TRF"));
        transfer.setMaterialId(inv.getMaterialId());
        transfer.setInventoryId(inventoryId);
        transfer.setQuantity(quantity);
        transfer.setFromLocation(inv.getLocation());
        transfer.setToLocation(toLocation);
        transfer.setTransferDate(LocalDateTime.now());
        transfer.setStatus("COMPLETED");
        transfer.setRemark(remark);
        transfer.setOperatorId(getCurrentUserId());

        inv.setLocation(toLocation);
        inventoryRepo.save(inv);

        return toTransferVO(transferRepo.save(transfer));
    }

    // ==================== 报损 ====================
    public PageResult<DamageVO> getDamageList(String keyword, Pageable pageable) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword;
        Page<InventoryDamage> page = damageRepo.findByConditions(kw, pageable);
        return PageResult.of(page.getContent().stream().map(this::toDamageVO).collect(Collectors.toList()),
                page.getTotalElements(), pageable.getPageNumber() + 1, pageable.getPageSize());
    }

    @Transactional
    public DamageVO createDamage(Long inventoryId, Integer quantity, String damageReason, String remark) {
        Inventory inv = inventoryRepo.findById(inventoryId)
                .orElseThrow(() -> new BusinessException("库存记录不存在"));
        if (inv.getQuantity() < quantity) throw new BusinessException("报损数量超出库存");

        InventoryDamage damage = new InventoryDamage();
        damage.setDamageNo(genNo("DMG"));
        damage.setMaterialId(inv.getMaterialId());
        damage.setInventoryId(inventoryId);
        damage.setBatchNumber(inv.getBatchNumber());
        damage.setQuantity(quantity);
        damage.setDamageReason(damageReason);
        damage.setDamageDate(LocalDateTime.now());
        damage.setStatus("CONFIRMED");
        damage.setRemark(remark);
        damage.setOperatorId(getCurrentUserId());

        inv.setQuantity(inv.getQuantity() - quantity);
        if (inv.getQuantity() == 0) inv.setStatus(0);
        inventoryRepo.save(inv);

        // 记录流水
        InventoryTransaction tx = new InventoryTransaction();
        tx.setMaterialId(inv.getMaterialId());
        tx.setTransactionType("DAMAGE");
        tx.setQuantity(quantity);
        tx.setBatchNumber(inv.getBatchNumber());
        tx.setRemark("报损：" + damageReason);
        tx.setOperatorId(getCurrentUserId());
        transactionRepo.save(tx);

        return toDamageVO(damageRepo.save(damage));
    }

    // ==================== 借用 ====================
    public PageResult<BorrowingVO> getBorrowingList(String keyword, String status, Pageable pageable) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword;
        String st = (status == null || status.isBlank()) ? null : status;
        Page<InventoryBorrowing> page = borrowingRepo.findByConditions(kw, st, pageable);
        return PageResult.of(page.getContent().stream().map(this::toBorrowingVO).collect(Collectors.toList()),
                page.getTotalElements(), pageable.getPageNumber() + 1, pageable.getPageSize());
    }

    @Transactional
    public BorrowingVO createBorrowing(Long inventoryId, Integer quantity, Long deptId, String borrowerName,
                                       LocalDate expectedReturnDate, String remark) {
        Inventory inv = inventoryRepo.findById(inventoryId)
                .orElseThrow(() -> new BusinessException("库存记录不存在"));
        if (inv.getQuantity() < quantity) throw new BusinessException("库存不足");

        InventoryBorrowing borrowing = new InventoryBorrowing();
        borrowing.setBorrowingNo(genNo("BRW"));
        borrowing.setMaterialId(inv.getMaterialId());
        borrowing.setInventoryId(inventoryId);
        borrowing.setBatchNumber(inv.getBatchNumber());
        borrowing.setQuantity(quantity);
        borrowing.setDeptId(deptId);
        borrowing.setBorrowerName(borrowerName);
        borrowing.setBorrowingDate(LocalDateTime.now());
        borrowing.setExpectedReturnDate(expectedReturnDate);
        borrowing.setStatus("BORROWED");
        borrowing.setRemark(remark);
        borrowing.setOperatorId(getCurrentUserId());

        inv.setQuantity(inv.getQuantity() - quantity);
        if (inv.getQuantity() == 0) inv.setStatus(0);
        inventoryRepo.save(inv);

        InventoryTransaction tx = new InventoryTransaction();
        tx.setMaterialId(inv.getMaterialId());
        tx.setTransactionType("BORROWING");
        tx.setQuantity(quantity);
        tx.setBatchNumber(inv.getBatchNumber());
        tx.setDeptId(deptId);
        tx.setRemark("借用给：" + borrowerName);
        tx.setOperatorId(getCurrentUserId());
        transactionRepo.save(tx);

        return toBorrowingVO(borrowingRepo.save(borrowing));
    }

    @Transactional
    public void returnBorrowing(Long id) {
        InventoryBorrowing b = borrowingRepo.findById(id)
                .orElseThrow(() -> new BusinessException("借用记录不存在"));
        if ("RETURNED".equals(b.getStatus())) throw new BusinessException("已归还");

        Inventory inv = inventoryRepo.findById(b.getInventoryId())
                .orElseThrow(() -> new BusinessException("库存记录不存在"));
        inv.setQuantity(inv.getQuantity() + b.getQuantity());
        inv.setStatus(1);
        inventoryRepo.save(inv);

        b.setStatus("RETURNED");
        b.setActualReturnDate(LocalDate.now());
        borrowingRepo.save(b);
    }

    // ==================== VO 转换 ====================
    private StocktakingVO toStocktakingVO(InventoryStocktaking s) {
        StocktakingVO vo = new StocktakingVO();
        vo.setId(s.getId());
        vo.setStocktakingNo(s.getStocktakingNo());
        vo.setStocktakingDate(s.getStocktakingDate());
        vo.setLocation(s.getLocation());
        vo.setStatus(s.getStatus());
        vo.setRemark(s.getRemark());
        vo.setCreatedBy(s.getCreatedBy());
        vo.setCreateTime(s.getCreateTime());
        if (s.getCreatedBy() != null) {
            userRepo.findById(s.getCreatedBy()).ifPresent(u -> vo.setCreatedByName(u.getRealName()));
        }
        return vo;
    }

    private TransferVO toTransferVO(InventoryTransfer t) {
        TransferVO vo = new TransferVO();
        vo.setId(t.getId());
        vo.setTransferNo(t.getTransferNo());
        vo.setMaterialId(t.getMaterialId());
        vo.setInventoryId(t.getInventoryId());
        vo.setQuantity(t.getQuantity());
        vo.setFromLocation(t.getFromLocation());
        vo.setToLocation(t.getToLocation());
        vo.setTransferDate(t.getTransferDate());
        vo.setStatus(t.getStatus());
        vo.setRemark(t.getRemark());
        vo.setCreateTime(t.getCreateTime());
        materialRepo.findById(t.getMaterialId()).ifPresent(m -> {
            vo.setMaterialName(m.getMaterialName());
            vo.setMaterialCode(m.getMaterialCode());
        });
        if (t.getOperatorId() != null) {
            userRepo.findById(t.getOperatorId()).ifPresent(u -> vo.setOperatorName(u.getRealName()));
        }
        return vo;
    }

    private DamageVO toDamageVO(InventoryDamage d) {
        DamageVO vo = new DamageVO();
        vo.setId(d.getId());
        vo.setDamageNo(d.getDamageNo());
        vo.setMaterialId(d.getMaterialId());
        vo.setBatchNumber(d.getBatchNumber());
        vo.setQuantity(d.getQuantity());
        vo.setDamageReason(d.getDamageReason());
        vo.setDamageDate(d.getDamageDate());
        vo.setStatus(d.getStatus());
        vo.setRemark(d.getRemark());
        vo.setCreateTime(d.getCreateTime());
        materialRepo.findById(d.getMaterialId()).ifPresent(m -> vo.setMaterialName(m.getMaterialName()));
        if (d.getOperatorId() != null) {
            userRepo.findById(d.getOperatorId()).ifPresent(u -> vo.setOperatorName(u.getRealName()));
        }
        return vo;
    }

    private BorrowingVO toBorrowingVO(InventoryBorrowing b) {
        BorrowingVO vo = new BorrowingVO();
        vo.setId(b.getId());
        vo.setBorrowingNo(b.getBorrowingNo());
        vo.setMaterialId(b.getMaterialId());
        vo.setQuantity(b.getQuantity());
        vo.setDeptId(b.getDeptId());
        vo.setBorrowerName(b.getBorrowerName());
        vo.setBorrowingDate(b.getBorrowingDate());
        vo.setExpectedReturnDate(b.getExpectedReturnDate());
        vo.setActualReturnDate(b.getActualReturnDate());
        vo.setStatus(b.getStatus());
        vo.setRemark(b.getRemark());
        vo.setCreateTime(b.getCreateTime());
        materialRepo.findById(b.getMaterialId()).ifPresent(m -> vo.setMaterialName(m.getMaterialName()));
        if (b.getDeptId() != null) {
            deptRepo.findById(b.getDeptId()).ifPresent(d -> vo.setDeptName(d.getDeptName()));
        }
        if (b.getOperatorId() != null) {
            userRepo.findById(b.getOperatorId()).ifPresent(u -> vo.setOperatorName(u.getRealName()));
        }
        return vo;
    }

    // ==================== Inner VO classes ====================
    @Data
    public static class StocktakingVO {
        private Long id;
        private String stocktakingNo;
        private LocalDateTime stocktakingDate;
        private String location;
        private String status;
        private String remark;
        private Long createdBy;
        private String createdByName;
        private LocalDateTime createTime;
        private List<StocktakingItemVO> items;
    }

    @Data
    public static class StocktakingItemVO {
        private Long id;
        private Long materialId;
        private String materialName;
        private String materialCode;
        private String specification;
        private String unit;
        private Long inventoryId;
        private String batchNumber;
        private Integer systemQuantity;
        private Integer actualQuantity;
        private Integer difference;
        private String remark;
    }

    @Data
    public static class TransferVO {
        private Long id;
        private String transferNo;
        private Long materialId;
        private String materialName;
        private String materialCode;
        private Long inventoryId;
        private Integer quantity;
        private String fromLocation;
        private String toLocation;
        private LocalDateTime transferDate;
        private String status;
        private String remark;
        private String operatorName;
        private LocalDateTime createTime;
    }

    @Data
    public static class DamageVO {
        private Long id;
        private String damageNo;
        private Long materialId;
        private String materialName;
        private String batchNumber;
        private Integer quantity;
        private String damageReason;
        private LocalDateTime damageDate;
        private String status;
        private String remark;
        private String operatorName;
        private LocalDateTime createTime;
    }

    @Data
    public static class BorrowingVO {
        private Long id;
        private String borrowingNo;
        private Long materialId;
        private String materialName;
        private Integer quantity;
        private Long deptId;
        private String deptName;
        private String borrowerName;
        private LocalDateTime borrowingDate;
        private LocalDate expectedReturnDate;
        private LocalDate actualReturnDate;
        private String status;
        private String remark;
        private String operatorName;
        private LocalDateTime createTime;
    }
}
