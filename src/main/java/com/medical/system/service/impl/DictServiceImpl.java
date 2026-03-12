package com.medical.system.service.impl;

import com.medical.system.common.PageResult;
import com.medical.system.entity.SysDict;
import com.medical.system.entity.SysDictItem;
import com.medical.system.exception.BusinessException;
import com.medical.system.repository.SysDictItemRepository;
import com.medical.system.repository.SysDictRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DictServiceImpl {

    private final SysDictRepository dictRepository;
    private final SysDictItemRepository dictItemRepository;

    public PageResult<SysDict> getDictTypes(String keyword, Pageable pageable) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword;
        Page<SysDict> page = dictRepository.findByConditions(kw, pageable);
        return PageResult.of(page.getContent(), page.getTotalElements(),
                pageable.getPageNumber() + 1, pageable.getPageSize());
    }

    public SysDict createDictType(SysDict dict) {
        if (dictRepository.findByDictCode(dict.getDictCode()).isPresent()) {
            throw new BusinessException("字典编码已存在: " + dict.getDictCode());
        }
        return dictRepository.save(dict);
    }

    public SysDict updateDictType(Long id, SysDict dict) {
        SysDict existing = dictRepository.findById(id)
                .orElseThrow(() -> new BusinessException("字典不存在"));
        existing.setDictName(dict.getDictName());
        existing.setDictType(dict.getDictType());
        existing.setRemark(dict.getRemark());
        existing.setStatus(dict.getStatus());
        return dictRepository.save(existing);
    }

    @Transactional
    public void deleteDictType(Long id) {
        dictItemRepository.deleteByDictId(id);
        dictRepository.deleteById(id);
    }

    public List<SysDictItem> getDictItemsByDictId(Long dictId) {
        return dictItemRepository.findByDictIdOrderBySortOrderAsc(dictId);
    }

    public List<SysDictItem> getDictItemsByCode(String dictCode) {
        SysDict dict = dictRepository.findByDictCode(dictCode)
                .orElseThrow(() -> new BusinessException("字典不存在: " + dictCode));
        return dictItemRepository.findByDictIdOrderBySortOrderAsc(dict.getId());
    }

    public SysDictItem createDictItem(SysDictItem item) {
        if (!dictRepository.existsById(item.getDictId())) {
            throw new BusinessException("字典不存在");
        }
        return dictItemRepository.save(item);
    }

    public SysDictItem updateDictItem(Long id, SysDictItem item) {
        SysDictItem existing = dictItemRepository.findById(id)
                .orElseThrow(() -> new BusinessException("字典项不存在"));
        existing.setItemLabel(item.getItemLabel());
        existing.setItemValue(item.getItemValue());
        existing.setSortOrder(item.getSortOrder());
        existing.setRemark(item.getRemark());
        existing.setStatus(item.getStatus());
        return dictItemRepository.save(existing);
    }

    public void deleteDictItem(Long id) {
        dictItemRepository.deleteById(id);
    }
}
