package com.medical.system.repository;

import com.medical.system.entity.SysDict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SysDictRepository extends JpaRepository<SysDict, Long> {
    Optional<SysDict> findByDictCode(String dictCode);

    @Query("SELECT d FROM SysDict d WHERE (:keyword IS NULL OR d.dictName LIKE %:keyword% OR d.dictCode LIKE %:keyword%)")
    Page<SysDict> findByConditions(@Param("keyword") String keyword, Pageable pageable);
}
