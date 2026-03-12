package com.medical.system.controller;

import com.medical.system.common.PageResult;
import com.medical.system.common.Result;
import com.medical.system.service.impl.InventoryExtServiceImpl;
import com.medical.system.service.impl.InventoryExtServiceImpl.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryExtController {

    private final InventoryExtServiceImpl inventoryExtService;

    // ========== 盘点 ==========
    @GetMapping("/stocktaking")
    @PreAuthorize("hasAuthority('menu:inventory')")
    public Result<PageResult<StocktakingVO>> getStocktakingList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(inventoryExtService.getStocktakingList(keyword, status,
                PageRequest.of(page - 1, size, Sort.by("id").descending())));
    }

    @PostMapping("/stocktaking")
    @PreAuthorize("hasAuthority('inventory:edit')")
    public Result<StocktakingVO> createStocktaking(@RequestBody Map<String, String> body) {
        return Result.success(inventoryExtService.createStocktaking(
                body.get("location"), body.get("remark")));
    }

    @GetMapping("/stocktaking/{id}")
    @PreAuthorize("hasAuthority('menu:inventory')")
    public Result<StocktakingVO> getStocktakingDetail(@PathVariable Long id) {
        return Result.success(inventoryExtService.getStocktakingDetail(id));
    }

    @PutMapping("/stocktaking/{id}/complete")
    @PreAuthorize("hasAuthority('inventory:edit')")
    public Result<Void> completeStocktaking(@PathVariable Long id, @RequestBody List<Map<String, Object>> items) {
        inventoryExtService.completeStocktaking(id, items);
        return Result.success();
    }

    // ========== 移库 ==========
    @GetMapping("/transfer")
    @PreAuthorize("hasAuthority('menu:inventory')")
    public Result<PageResult<TransferVO>> getTransferList(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(inventoryExtService.getTransferList(keyword,
                PageRequest.of(page - 1, size, Sort.by("id").descending())));
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasAuthority('inventory:edit')")
    public Result<TransferVO> createTransfer(@RequestBody Map<String, Object> body) {
        Long inventoryId = Long.valueOf(body.get("inventoryId").toString());
        Integer quantity = Integer.valueOf(body.get("quantity").toString());
        String toLocation = body.get("toLocation").toString();
        String remark = body.containsKey("remark") ? body.get("remark").toString() : null;
        return Result.success(inventoryExtService.createTransfer(inventoryId, quantity, toLocation, remark));
    }

    // ========== 报损 ==========
    @GetMapping("/damage")
    @PreAuthorize("hasAuthority('menu:inventory')")
    public Result<PageResult<DamageVO>> getDamageList(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(inventoryExtService.getDamageList(keyword,
                PageRequest.of(page - 1, size, Sort.by("id").descending())));
    }

    @PostMapping("/damage")
    @PreAuthorize("hasAuthority('inventory:edit')")
    public Result<DamageVO> createDamage(@RequestBody Map<String, Object> body) {
        Long inventoryId = Long.valueOf(body.get("inventoryId").toString());
        Integer quantity = Integer.valueOf(body.get("quantity").toString());
        String damageReason = body.get("damageReason").toString();
        String remark = body.containsKey("remark") ? body.get("remark").toString() : null;
        return Result.success(inventoryExtService.createDamage(inventoryId, quantity, damageReason, remark));
    }

    // ========== 借用 ==========
    @GetMapping("/borrowing")
    @PreAuthorize("hasAuthority('menu:inventory')")
    public Result<PageResult<BorrowingVO>> getBorrowingList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(inventoryExtService.getBorrowingList(keyword, status,
                PageRequest.of(page - 1, size, Sort.by("id").descending())));
    }

    @PostMapping("/borrowing")
    @PreAuthorize("hasAuthority('inventory:edit')")
    public Result<BorrowingVO> createBorrowing(@RequestBody Map<String, Object> body) {
        Long inventoryId = Long.valueOf(body.get("inventoryId").toString());
        Integer quantity = Integer.valueOf(body.get("quantity").toString());
        Long deptId = body.containsKey("deptId") && body.get("deptId") != null
                ? Long.valueOf(body.get("deptId").toString()) : null;
        String borrowerName = body.get("borrowerName").toString();
        LocalDate expectedReturnDate = body.containsKey("expectedReturnDate") && body.get("expectedReturnDate") != null
                ? LocalDate.parse(body.get("expectedReturnDate").toString()) : null;
        String remark = body.containsKey("remark") ? body.get("remark") != null ? body.get("remark").toString() : null : null;
        return Result.success(inventoryExtService.createBorrowing(inventoryId, quantity, deptId, borrowerName, expectedReturnDate, remark));
    }

    @PutMapping("/borrowing/{id}/return")
    @PreAuthorize("hasAuthority('inventory:edit')")
    public Result<Void> returnBorrowing(@PathVariable Long id) {
        inventoryExtService.returnBorrowing(id);
        return Result.success();
    }
}
