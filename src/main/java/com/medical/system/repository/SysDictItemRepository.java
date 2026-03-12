package com.medical.system.repository;

import com.medical.system.entity.SysDictItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SysDictItemRepository extends JpaRepository<SysDictItem, Long> {
    List<SysDictItem> findByDictIdOrderBySortOrderAsc(Long dictId);

    void deleteByDictId(Long dictId);
}
