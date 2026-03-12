package com.medical.system.service.impl;

import com.medical.system.common.PageResult;
import com.medical.system.dto.request.CreateMaterialRequest;
import com.medical.system.dto.response.MaterialResponse;
import com.medical.system.entity.Material;
import com.medical.system.exception.BusinessException;
import com.medical.system.repository.InventoryRepository;
import com.medical.system.repository.MaterialRepository;
import com.medical.system.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaterialServiceImpl {

    private final MaterialRepository materialRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryRepository inventoryRepository;

    public PageResult<MaterialResponse> getMaterials(String keyword, String category, Integer status, Pageable pageable) {
        Page<Material> page = materialRepository.findByConditions(keyword, category, status, pageable);
        List<MaterialResponse> records = page.getContent().stream()
                .map(this::convertToResponse).collect(Collectors.toList());
        return PageResult.of(records, page.getTotalElements(), pageable.getPageNumber() + 1, pageable.getPageSize());
    }

    public MaterialResponse getMaterialById(Long id) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new BusinessException("耗材不存在"));
        return convertToResponse(material);
    }

    public List<MaterialResponse> getActiveMaterials() {
        return materialRepository.findByStatus(1).stream()
                .map(this::convertToResponse).collect(Collectors.toList());
    }

    @Transactional
    public MaterialResponse createMaterial(CreateMaterialRequest request) {
        if (materialRepository.existsByMaterialCode(request.getMaterialCode())) {
            throw new BusinessException("耗材编码已存在");
        }
        Material material = new Material();
        applyRequest(material, request);
        material.setMaterialCode(request.getMaterialCode());
        return convertToResponse(materialRepository.save(material));
    }

    @Transactional
    public MaterialResponse updateMaterial(Long id, CreateMaterialRequest request) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new BusinessException("耗材不存在"));
        applyRequest(material, request);
        return convertToResponse(materialRepository.save(material));
    }

    private void applyRequest(Material material, CreateMaterialRequest request) {
        material.setMaterialName(request.getMaterialName());
        material.setCategory(request.getCategory());
        material.setSpecification(request.getSpecification());
        material.setUnit(request.getUnit());
        material.setSupplierId(request.getSupplierId());
        material.setStandardPrice(request.getStandardPrice());
        material.setMinStock(request.getMinStock());
        material.setMaxStock(request.getMaxStock());
        material.setLeadTime(request.getLeadTime());
        material.setDescription(request.getDescription());
        material.setRegistrationNo(request.getRegistrationNo());
        material.setRegistrationExpiry(request.getRegistrationExpiry());
        material.setManufacturer(request.getManufacturer());
        material.setIsHighValue(Boolean.TRUE.equals(request.getIsHighValue()));
    }

    @Transactional
    public void deleteMaterial(Long id) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new BusinessException("耗材不存在"));
        material.setStatus(0);
        materialRepository.save(material);
    }

    public MaterialResponse convertToResponse(Material material) {
        MaterialResponse response = new MaterialResponse();
        response.setId(material.getId());
        response.setMaterialCode(material.getMaterialCode());
        response.setMaterialName(material.getMaterialName());
        response.setCategory(material.getCategory());
        response.setSpecification(material.getSpecification());
        response.setUnit(material.getUnit());
        response.setSupplierId(material.getSupplierId());
        response.setStandardPrice(material.getStandardPrice());
        response.setMinStock(material.getMinStock());
        response.setMaxStock(material.getMaxStock());
        response.setLeadTime(material.getLeadTime());
        response.setDescription(material.getDescription());
        response.setRegistrationNo(material.getRegistrationNo());
        response.setRegistrationExpiry(material.getRegistrationExpiry());
        response.setManufacturer(material.getManufacturer());
        response.setIsHighValue(material.getIsHighValue());
        response.setStatus(material.getStatus());
        response.setCreateTime(material.getCreateTime());
        if (material.getSupplierId() != null) {
            supplierRepository.findById(material.getSupplierId())
                    .ifPresent(s -> response.setSupplierName(s.getSupplierName()));
        }
        Integer stock = inventoryRepository.sumQuantityByMaterialId(material.getId());
        response.setCurrentStock(stock != null ? stock : 0);
        return response;
    }
}
