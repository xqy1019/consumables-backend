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
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UdiServiceImpl {

    private final MaterialUdiRepository udiRepo;
    private final MaterialRepository materialRepo;
    private final InventoryRepository inventoryRepo;
    private final SupplierRepository supplierRepo;

    public PageResult<UdiVO> getUdiList(String keyword, String status, Pageable pageable) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword;
        String st = (status == null || status.isBlank()) ? null : status;
        Page<MaterialUdi> page = udiRepo.findByConditions(kw, st, pageable);
        return PageResult.of(page.getContent().stream().map(this::toVO).collect(Collectors.toList()),
                page.getTotalElements(), pageable.getPageNumber() + 1, pageable.getPageSize());
    }

    public UdiVO createUdi(MaterialUdi udi) {
        if (udiRepo.findByUdiCode(udi.getUdiCode()).isPresent()) {
            throw new BusinessException("UDI码已存在：" + udi.getUdiCode());
        }
        if (!materialRepo.existsById(udi.getMaterialId())) {
            throw new BusinessException("耗材不存在");
        }
        return toVO(udiRepo.save(udi));
    }

    public UdiVO scanUdi(String udiCode) {
        MaterialUdi udi = udiRepo.findByUdiCode(udiCode)
                .orElseThrow(() -> new BusinessException("未找到UDI：" + udiCode));
        return toVO(udi);
    }

    public void updateUdiStatus(Long id, String status) {
        MaterialUdi udi = udiRepo.findById(id).orElseThrow(() -> new BusinessException("UDI不存在"));
        udi.setStatus(status);
        udiRepo.save(udi);
    }

    public UdiVO toVO(MaterialUdi udi) {
        UdiVO vo = new UdiVO();
        vo.setId(udi.getId());
        vo.setMaterialId(udi.getMaterialId());
        vo.setInventoryId(udi.getInventoryId());
        vo.setUdiCode(udi.getUdiCode());
        vo.setBatchNumber(udi.getBatchNumber());
        vo.setSerialNumber(udi.getSerialNumber());
        vo.setManufactureDate(udi.getManufactureDate());
        vo.setExpiryDate(udi.getExpiryDate());
        vo.setSupplierId(udi.getSupplierId());
        vo.setStatus(udi.getStatus());
        vo.setRemark(udi.getRemark());
        vo.setCreateTime(udi.getCreateTime());
        materialRepo.findById(udi.getMaterialId()).ifPresent(m -> {
            vo.setMaterialName(m.getMaterialName());
            vo.setMaterialCode(m.getMaterialCode());
            vo.setSpecification(m.getSpecification());
            vo.setUnit(m.getUnit());
        });
        if (udi.getSupplierId() != null) {
            supplierRepo.findById(udi.getSupplierId()).ifPresent(s -> vo.setSupplierName(s.getSupplierName()));
        }
        return vo;
    }

    @Data
    public static class UdiVO {
        private Long id;
        private Long materialId;
        private String materialName;
        private String materialCode;
        private String specification;
        private String unit;
        private Long inventoryId;
        private String udiCode;
        private String batchNumber;
        private String serialNumber;
        private LocalDate manufactureDate;
        private LocalDate expiryDate;
        private Long supplierId;
        private String supplierName;
        private String status;
        private String remark;
        private LocalDateTime createTime;
    }
}
