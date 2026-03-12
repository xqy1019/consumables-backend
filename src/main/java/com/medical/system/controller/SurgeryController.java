package com.medical.system.controller;

import com.medical.system.common.PageResult;
import com.medical.system.common.Result;
import com.medical.system.entity.SurgeryRecord;
import com.medical.system.service.impl.SurgeryServiceImpl;
import com.medical.system.service.impl.SurgeryServiceImpl.SurgeryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/surgery")
@RequiredArgsConstructor
public class SurgeryController {

    private final SurgeryServiceImpl surgeryService;

    @GetMapping
    @PreAuthorize("hasAuthority('menu:tracing')")
    public Result<PageResult<SurgeryVO>> getList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long deptId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(surgeryService.getSurgeryList(keyword, deptId,
                PageRequest.of(page - 1, size, Sort.by("surgeryDate").descending())));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('inventory:edit')")
    public Result<SurgeryVO> create(@RequestBody SurgeryRecord surgery) {
        return Result.success(surgeryService.createSurgery(surgery));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('menu:tracing')")
    public Result<SurgeryVO> getDetail(@PathVariable Long id) {
        return Result.success(surgeryService.getSurgeryDetail(id));
    }

    @PutMapping("/{id}/bind")
    @PreAuthorize("hasAuthority('inventory:edit')")
    public Result<Void> bindMaterials(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> bindings = (List<Map<String, Object>>) body.get("bindings");
        surgeryService.bindMaterials(id, bindings);
        return Result.success();
    }
}
