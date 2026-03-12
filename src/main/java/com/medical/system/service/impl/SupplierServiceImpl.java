package com.medical.system.service.impl;

import com.medical.system.common.PageResult;
import com.medical.system.dto.request.CreateSupplierRequest;
import com.medical.system.dto.response.SupplierResponse;
import com.medical.system.entity.Supplier;
import com.medical.system.exception.BusinessException;
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
public class SupplierServiceImpl {

    private final SupplierRepository supplierRepository;

    public PageResult<SupplierResponse> getSuppliers(String keyword, Pageable pageable) {
        Page<Supplier> page = supplierRepository.findByConditions(keyword, pageable);
        List<SupplierResponse> records = page.getContent().stream()
                .map(this::convertToResponse).collect(Collectors.toList());
        return PageResult.of(records, page.getTotalElements(), pageable.getPageNumber() + 1, pageable.getPageSize());
    }

    public SupplierResponse getSupplierById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new BusinessException("供应商不存在"));
        return convertToResponse(supplier);
    }

    public List<SupplierResponse> getActiveSuppliers() {
        return supplierRepository.findByStatus(1).stream()
                .map(this::convertToResponse).collect(Collectors.toList());
    }

    @Transactional
    public SupplierResponse createSupplier(CreateSupplierRequest request) {
        if (supplierRepository.existsBySupplierCode(request.getSupplierCode())) {
            throw new BusinessException("供应商编码已存在");
        }
        Supplier supplier = new Supplier();
        supplier.setSupplierName(request.getSupplierName());
        supplier.setSupplierCode(request.getSupplierCode());
        applyRequest(supplier, request);
        return convertToResponse(supplierRepository.save(supplier));
    }

    @Transactional
    public SupplierResponse updateSupplier(Long id, CreateSupplierRequest request) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new BusinessException("供应商不存在"));
        supplier.setSupplierName(request.getSupplierName());
        applyRequest(supplier, request);
        return convertToResponse(supplierRepository.save(supplier));
    }

    private void applyRequest(Supplier supplier, CreateSupplierRequest request) {
        supplier.setContactPerson(request.getContactPerson());
        supplier.setPhone(request.getPhone());
        supplier.setEmail(request.getEmail());
        supplier.setAddress(request.getAddress());
        supplier.setLicenseNo(request.getLicenseNo());
        supplier.setLicenseExpiry(request.getLicenseExpiry());
    }

    @Transactional
    public void deleteSupplier(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new BusinessException("供应商不存在"));
        supplier.setStatus(0);
        supplierRepository.save(supplier);
    }

    public SupplierResponse convertToResponse(Supplier supplier) {
        SupplierResponse response = new SupplierResponse();
        response.setId(supplier.getId());
        response.setSupplierName(supplier.getSupplierName());
        response.setSupplierCode(supplier.getSupplierCode());
        response.setContactPerson(supplier.getContactPerson());
        response.setPhone(supplier.getPhone());
        response.setEmail(supplier.getEmail());
        response.setAddress(supplier.getAddress());
        response.setLicenseNo(supplier.getLicenseNo());
        response.setLicenseExpiry(supplier.getLicenseExpiry());
        response.setStatus(supplier.getStatus());
        response.setCreateTime(supplier.getCreateTime());
        return response;
    }
}
