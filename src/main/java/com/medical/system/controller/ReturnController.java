package com.medical.system.controller;

import com.medical.system.common.PageResult;
import com.medical.system.common.Result;
import com.medical.system.security.SecurityUtils;
import com.medical.system.service.impl.ReturnServiceImpl;
import com.medical.system.service.impl.ReturnServiceImpl.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/return-requests")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnServiceImpl returnService;

    @GetMapping
    @PreAuthorize("hasAuthority('menu:inventory')")
    public Result<PageResult<ReturnVO>> getList(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long deptId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by("createTime").descending());
        return Result.success(returnService.getReturnRequests(status, deptId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('menu:inventory')")
    public Result<ReturnVO> getById(@PathVariable Long id) {
        return Result.success(returnService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('menu:inventory')")
    public Result<ReturnVO> create(@RequestBody CreateReturnRequest request) {
        return Result.success(returnService.createReturn(request, SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('inventory:edit')")
    public Result<ReturnVO> approve(@PathVariable Long id,
                                     @RequestBody ApproveReturnRequest request) {
        return Result.success(returnService.approve(id, SecurityUtils.getCurrentUserId(),
                request.isApproved(), request.getRemark()));
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('inventory:edit')")
    public Result<ReturnVO> complete(@PathVariable Long id) {
        return Result.success(returnService.complete(id, SecurityUtils.getCurrentUserId()));
    }
}
