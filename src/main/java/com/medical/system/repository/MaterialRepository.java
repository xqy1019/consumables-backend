package com.medical.system.repository;

import com.medical.system.entity.Material;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    boolean existsByMaterialCode(String materialCode);
    List<Material> findByStatus(Integer status);

    long countByStatus(Integer status);

    @Query("SELECT m FROM Material m WHERE " +
           "(:keyword IS NULL OR m.materialName LIKE %:keyword% OR m.materialCode LIKE %:keyword%) AND " +
           "(:category IS NULL OR m.category = :category) AND " +
           "(:status IS NULL OR m.status = :status)")
    Page<Material> findByConditions(@Param("keyword") String keyword,
                                    @Param("category") String category,
                                    @Param("status") Integer status,
                                    Pageable pageable);

    @Query("SELECT m.category, COUNT(m) FROM Material m " +
           "WHERE m.status = 1 AND m.category IS NOT NULL " +
           "GROUP BY m.category ORDER BY COUNT(m) DESC")
    List<Object[]> findActiveMaterialsByCategory();
}
