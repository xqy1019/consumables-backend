package com.medical.system.repository;

import com.medical.system.entity.ReturnRequestItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReturnRequestItemRepository extends JpaRepository<ReturnRequestItem, Long> {
    List<ReturnRequestItem> findByReturnId(Long returnId);
}
