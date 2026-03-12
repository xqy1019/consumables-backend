package com.medical.system.repository;

import com.medical.system.entity.MaterialUdi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MaterialUdiRepository extends JpaRepository<MaterialUdi, Long> {
    Optional<MaterialUdi> findByUdiCode(String udiCode);

    List<MaterialUdi> findByMaterialId(Long materialId);

    @Query("SELECT u FROM MaterialUdi u WHERE (:keyword IS NULL OR u.udiCode LIKE %:keyword% OR u.batchNumber LIKE %:keyword%) AND (:status IS NULL OR u.status = :status)")
    Page<MaterialUdi> findByConditions(@Param("keyword") String keyword, @Param("status") String status, Pageable pageable);
}
