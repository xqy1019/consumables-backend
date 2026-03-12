package com.medical.system.controller;

import com.medical.system.common.PageResult;
import com.medical.system.common.Result;
import com.medical.system.dto.request.CreateMaterialRequest;
import com.medical.system.dto.response.MaterialResponse;
import com.medical.system.service.impl.MaterialServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialServiceImpl materialService;

    @GetMapping
    @PreAuthorize("hasAuthority('menu:material')")
    public Result<PageResult<MaterialResponse>> getMaterials(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by("createTime").descending());
        return Result.success(materialService.getMaterials(keyword, category, status, pageable));
    }

    @GetMapping("/active")
    public Result<List<MaterialResponse>> getActiveMaterials() {
        return Result.success(materialService.getActiveMaterials());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('menu:material')")
    public Result<MaterialResponse> getMaterialById(@PathVariable Long id) {
        return Result.success(materialService.getMaterialById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('material:edit')")
    public Result<MaterialResponse> createMaterial(@Valid @RequestBody CreateMaterialRequest request) {
        return Result.success(materialService.createMaterial(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('material:edit')")
    public Result<MaterialResponse> updateMaterial(@PathVariable Long id,
                                                    @RequestBody CreateMaterialRequest request) {
        return Result.success(materialService.updateMaterial(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('material:edit')")
    public Result<Void> deleteMaterial(@PathVariable Long id) {
        materialService.deleteMaterial(id);
        return Result.success();
    }
}
