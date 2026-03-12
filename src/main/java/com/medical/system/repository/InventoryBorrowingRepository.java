package com.medical.system.repository;

import com.medical.system.entity.InventoryBorrowing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InventoryBorrowingRepository extends JpaRepository<InventoryBorrowing, Long> {
    @Query("SELECT b FROM InventoryBorrowing b WHERE (:status IS NULL OR b.status = :status) AND (:keyword IS NULL OR b.borrowingNo LIKE %:keyword% OR b.borrowerName LIKE %:keyword%)")
    Page<InventoryBorrowing> findByConditions(@Param("keyword") String keyword, @Param("status") String status, Pageable pageable);

    List<InventoryBorrowing> findByStatus(String status);
}
