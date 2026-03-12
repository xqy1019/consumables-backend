package com.medical.system.repository;

import com.medical.system.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    boolean existsBySupplierCode(String supplierCode);
    List<Supplier> findByStatus(Integer status);

    @Query("SELECT s FROM Supplier s WHERE " +
           "(:keyword IS NULL OR s.supplierName LIKE %:keyword% OR s.supplierCode LIKE %:keyword%)")
    Page<Supplier> findByConditions(@Param("keyword") String keyword, Pageable pageable);
}
