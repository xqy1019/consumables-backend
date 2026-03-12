package com.medical.system.controller;

import com.medical.system.common.PageResult;
import com.medical.system.common.Result;
import com.medical.system.security.SecurityUtils;
import com.medical.system.service.impl.RecallServiceImpl;
import com.medical.system.service.impl.RecallServiceImpl.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recall")
@RequiredArgsConstructor
public class RecallController {

    private final RecallServiceImpl recallService;

    @GetMapping
    @PreAuthorize("hasAuthority('menu:inventory')")
    public Result<PageResult<RecallVO>> getRecalls(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by("createTime").descending());
        return Result.success(recallService.getRecalls(status, keyword, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('menu:inventory')")
    public Result<RecallDetailVO> getDetail(@PathVariable Long id) {
        return Result.success(recallService.getDetail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('inventory:edit')")
    public Result<RecallVO> createRecall(@RequestBody CreateRecallRequest request) {
        return Result.success(recallService.createRecall(request, SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/{id}/close")
    @PreAuthorize("hasAuthority('inventory:edit')")
    public Result<Void> closeRecall(@PathVariable Long id) {
        recallService.closeRecall(id);
        return Result.success();
    }

    @PostMapping("/{id}/disposal")
    @PreAuthorize("hasAuthority('inventory:edit')")
    public Result<DisposalVO> addDisposal(@PathVariable Long id,
                                           @RequestBody AddDisposalRequest request) {
        return Result.success(recallService.addDisposal(id, request, SecurityUtils.getCurrentUserId()));
    }
}
