package com.medical.system.controller;

import com.medical.system.common.PageResult;
import com.medical.system.common.Result;
import com.medical.system.entity.SysDict;
import com.medical.system.entity.SysDictItem;
import com.medical.system.service.impl.DictServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dict")
@RequiredArgsConstructor
public class DictController {

    private final DictServiceImpl dictService;

    // 字典类型列表：字典管理页专用
    @GetMapping("/types")
    @PreAuthorize("hasAuthority('menu:dict')")
    public Result<PageResult<SysDict>> getDictTypes(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return Result.success(dictService.getDictTypes(keyword, pageable));
    }

    // 管理类接口：仅限有 menu:dict 权限的角色
    @PostMapping("/types")
    @PreAuthorize("hasAuthority('menu:dict')")
    public Result<SysDict> createDictType(@RequestBody SysDict dict) {
        return Result.success(dictService.createDictType(dict));
    }

    @PutMapping("/types/{id}")
    @PreAuthorize("hasAuthority('menu:dict')")
    public Result<SysDict> updateDictType(@PathVariable Long id, @RequestBody SysDict dict) {
        return Result.success(dictService.updateDictType(id, dict));
    }

    @DeleteMapping("/types/{id}")
    @PreAuthorize("hasAuthority('menu:dict')")
    public Result<Void> deleteDictType(@PathVariable Long id) {
        dictService.deleteDictType(id);
        return Result.success();
    }

    // 查找类接口：用于各页面表单的分类/类型下拉，所有已认证用户可访问
    @GetMapping("/items/{dictCode}")
    public Result<List<SysDictItem>> getDictItemsByCode(@PathVariable String dictCode) {
        return Result.success(dictService.getDictItemsByCode(dictCode));
    }

    @GetMapping("/items")
    public Result<List<SysDictItem>> getDictItems(@RequestParam Long dictId) {
        return Result.success(dictService.getDictItemsByDictId(dictId));
    }

    // 字典项管理：仅限有 menu:dict 权限的角色
    @PostMapping("/items")
    @PreAuthorize("hasAuthority('menu:dict')")
    public Result<SysDictItem> createDictItem(@RequestBody SysDictItem item) {
        return Result.success(dictService.createDictItem(item));
    }

    @PutMapping("/items/{id}")
    @PreAuthorize("hasAuthority('menu:dict')")
    public Result<SysDictItem> updateDictItem(@PathVariable Long id, @RequestBody SysDictItem item) {
        return Result.success(dictService.updateDictItem(id, item));
    }

    @DeleteMapping("/items/{id}")
    @PreAuthorize("hasAuthority('menu:dict')")
    public Result<Void> deleteDictItem(@PathVariable Long id) {
        dictService.deleteDictItem(id);
        return Result.success();
    }
}
