package com.medical.system.service.impl;

import com.medical.system.common.PageResult;
import com.medical.system.dto.request.InboundRequest;
import com.medical.system.dto.request.InspectRequest;
import com.medical.system.dto.request.OutboundRequest;
import com.medical.system.dto.response.InventoryResponse;
import com.medical.system.entity.Inventory;
import com.medical.system.entity.InventoryTransaction;
import com.medical.system.entity.Material;
import com.medical.system.exception.BusinessException;
import com.medical.system.exception.ResourceNotFoundException;
import com.medical.system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl {

    private final InventoryRepository inventoryRepository;
    private final MaterialRepository materialRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final InventoryTransactionRepository transactionRepository;

    public PageResult<InventoryResponse> getInventory(String keyword, Integer status, Pageable pageable) {
        Page<Inventory> page = inventoryRepository.findByConditions(
                (keyword != null && keyword.isBlank()) ? null : keyword, status, pageable);
        List<InventoryResponse> records = convertToResponseBatch(page.getContent());
        return PageResult.of(records, page.getTotalElements(), pageable.getPageNumber() + 1, pageable.getPageSize());
    }

    public List<InventoryResponse> getAlerts() {
        LocalDate alertDate = LocalDate.now().plusDays(30);
        return convertToResponseBatch(inventoryRepository.findExpiringInventory(alertDate));
    }

    /**
     * 批量转换，通过 IN 查询批量加载关联数据，避免 N+1 问题。
     */
    private List<InventoryResponse> convertToResponseBatch(List<Inventory> inventories) {
        if (inventories.isEmpty()) return List.of();

        List<Long> materialIds = inventories.stream().map(Inventory::getMaterialId).distinct().collect(Collectors.toList());
        List<Long> supplierIds = inventories.stream().map(Inventory::getSupplierId)
                .filter(id -> id != null).distinct().collect(Collectors.toList());

        // 批量加载耗材
        Map<Long, com.medical.system.entity.Material> materialMap = materialRepository.findAllById(materialIds).stream()
                .collect(Collectors.toMap(com.medical.system.entity.Material::getId, Function.identity()));

        // 批量统计总库存（一次 GROUP BY 查询）
        Map<Long, Integer> stockMap = inventoryRepository.sumQuantityGroupByMaterialIds(materialIds).stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).intValue()
                ));

        // 批量加载供应商
        Map<Long, String> supplierNameMap = supplierIds.isEmpty() ? Map.of() :
                supplierRepository.findAllById(supplierIds).stream()
                        .collect(Collectors.toMap(
                                s -> s.getId(),
                                s -> s.getSupplierName()
                        ));

        LocalDate today = LocalDate.now();
        LocalDate alertDate = today.plusDays(30);

        return inventories.stream().map(inv -> {
            InventoryResponse response = new InventoryResponse();
            response.setId(inv.getId());
            response.setMaterialId(inv.getMaterialId());
            response.setBatchNumber(inv.getBatchNumber());
            response.setQuantity(inv.getQuantity());
            response.setLocation(inv.getLocation());
            response.setManufactureDate(inv.getManufactureDate());
            response.setExpiryDate(inv.getExpiryDate());
            response.setSupplierId(inv.getSupplierId());
            response.setReceiveDate(inv.getReceiveDate());
            response.setInspectionStatus(inv.getInspectionStatus());
            response.setInspectionRemark(inv.getInspectionRemark());
            response.setInspectTime(inv.getInspectTime());
            response.setStatus(inv.getStatus());
            response.setCreateTime(inv.getCreateTime());

            com.medical.system.entity.Material m = materialMap.get(inv.getMaterialId());
            if (m != null) {
                response.setMaterialName(m.getMaterialName());
                response.setMaterialCode(m.getMaterialCode());
                response.setSpecification(m.getSpecification());
                response.setUnit(m.getUnit());
                Integer totalStock = stockMap.getOrDefault(m.getId(), 0);
                response.setLowStock(m.getMinStock() != null && totalStock < m.getMinStock());
            }

            if (inv.getSupplierId() != null) {
                response.setSupplierName(supplierNameMap.get(inv.getSupplierId()));
            }

            if (inv.getExpiryDate() != null) {
                response.setExpiring(inv.getExpiryDate().isBefore(alertDate));
            }

            return response;
        }).collect(Collectors.toList());
    }

    public PageResult<InventoryResponse> getInspections(String keyword, String inspectionStatus, Pageable pageable) {
        String kw = (keyword != null && keyword.isBlank()) ? null : keyword;
        String status = (inspectionStatus == null || inspectionStatus.isBlank()) ? null : inspectionStatus;
        Page<Inventory> page = inventoryRepository.findByInspectionConditions(kw, status, pageable);
        return PageResult.of(convertToResponseBatch(page.getContent()), page.getTotalElements(),
                pageable.getPageNumber() + 1, pageable.getPageSize());
    }

    @Transactional
    public InventoryResponse inspect(Long id, InspectRequest request, Long inspectorId) {
        Inventory inv = inventoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("库存记录不存在"));
        inv.setInspectionStatus(request.getInspectionStatus());
        inv.setInspectionRemark(request.getInspectionRemark());
        inv.setInspectorId(inspectorId);
        inv.setInspectTime(java.time.LocalDateTime.now());
        if ("REJECTED".equals(request.getInspectionStatus())) {
            inv.setStatus(0);
        } else if ("PASSED".equals(request.getInspectionStatus())) {
            inv.setStatus(1);
        }
        return convertToResponseBatch(List.of(inventoryRepository.save(inv))).get(0);
    }

    public List<InventoryResponse> getBatchSuggestion(Long materialId) {
        List<Inventory> batches = inventoryRepository.findAvailableByMaterialIdFEFO(materialId);
        return convertToResponseBatch(batches);
    }

    @Transactional
    public InventoryResponse inbound(InboundRequest request) {
        if (!materialRepository.existsById(request.getMaterialId())) {
            throw new ResourceNotFoundException("耗材", request.getMaterialId());
        }
        Inventory inventory = new Inventory();
        inventory.setMaterialId(request.getMaterialId());
        inventory.setBatchNumber(request.getBatchNumber() != null ? request.getBatchNumber()
                : "BATCH" + System.currentTimeMillis());
        inventory.setQuantity(request.getQuantity());
        inventory.setLocation(request.getLocation());
        inventory.setManufactureDate(request.getManufactureDate());
        inventory.setExpiryDate(request.getExpiryDate());
        inventory.setSupplierId(request.getSupplierId());
        inventory.setReceiveDate(request.getReceiveDate() != null ? request.getReceiveDate() : LocalDate.now());
        inventory.setInspectionStatus(request.getInspectionStatus() != null ? request.getInspectionStatus() : "PASSED");
        inventory.setInspectionRemark(request.getInspectionRemark());
        inventory.setInspectorId(request.getInspectorId());
        if (request.getInspectorId() != null) {
            inventory.setInspectTime(java.time.LocalDateTime.now());
        }
        // 验收不合格的库存状态设为暂停（不可出库）
        if ("REJECTED".equals(inventory.getInspectionStatus())) {
            inventory.setStatus(0);
        }

        Inventory saved = inventoryRepository.save(inventory);

        // 记录流水
        InventoryTransaction tx = new InventoryTransaction();
        tx.setMaterialId(request.getMaterialId());
        tx.setTransactionType("INBOUND");
        tx.setQuantity(request.getQuantity());
        tx.setBatchNumber(saved.getBatchNumber());
        tx.setRemark(request.getRemark());
        transactionRepository.save(tx);

        return convertToResponseBatch(List.of(saved)).get(0);
    }

    @Transactional
    public void outbound(OutboundRequest request) {
        // 使用悲观写锁，防止并发出库超卖
        Inventory inventory = inventoryRepository.findByIdForUpdate(request.getInventoryId())
                .orElseThrow(() -> new BusinessException("库存记录不存在"));
        if (inventory.getQuantity() < request.getQuantity()) {
            throw new BusinessException("库存不足，当前库存: " + inventory.getQuantity());
        }
        inventory.setQuantity(inventory.getQuantity() - request.getQuantity());
        if (inventory.getQuantity() == 0) {
            inventory.setStatus(0);
        }
        inventoryRepository.save(inventory);

        // 记录流水
        InventoryTransaction tx = new InventoryTransaction();
        tx.setMaterialId(inventory.getMaterialId());
        tx.setTransactionType("OUTBOUND");
        tx.setQuantity(request.getQuantity());
        tx.setBatchNumber(inventory.getBatchNumber());
        tx.setDeptId(request.getDeptId());
        tx.setRequisitionId(request.getRequisitionId());
        tx.setRemark(request.getRemark());
        transactionRepository.save(tx);
    }

}
