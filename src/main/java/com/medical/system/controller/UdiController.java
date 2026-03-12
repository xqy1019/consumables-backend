package com.medical.system.controller;

import com.medical.system.common.PageResult;
import com.medical.system.common.Result;
import com.medical.system.entity.MaterialUdi;
import com.medical.system.service.impl.UdiServiceImpl;
import com.medical.system.service.impl.UdiServiceImpl.UdiVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/udi")
@RequiredArgsConstructor
public class UdiController {

    private final UdiServiceImpl udiService;

    @GetMapping
    @PreAuthorize("hasAuthority('menu:tracing')")
    public Result<PageResult<UdiVO>> getList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(udiService.getUdiList(keyword, status,
                PageRequest.of(page - 1, size, Sort.by("id").descending())));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('inventory:edit')")
    public Result<UdiVO> create(@RequestBody MaterialUdi udi) {
        return Result.success(udiService.createUdi(udi));
    }

    @GetMapping("/scan/{udiCode}")
    @PreAuthorize("hasAuthority('menu:tracing')")
    public Result<UdiVO> scan(@PathVariable String udiCode) {
        return Result.success(udiService.scanUdi(udiCode));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('inventory:edit')")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        udiService.updateUdiStatus(id, body.get("status"));
        return Result.success();
    }
}
