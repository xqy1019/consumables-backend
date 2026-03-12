package com.medical.system.repository;

import com.medical.system.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    @Query(value = "SELECT i.* FROM inventory i " +
                   "JOIN materials m ON m.id = i.material_id " +
                   "WHERE (:keyword IS NULL OR m.material_name ILIKE '%' || :keyword || '%' " +
                   "    OR m.material_code ILIKE '%' || :keyword || '%') " +
                   "AND (:status IS NULL OR i.status = :status) " +
                   "ORDER BY i.create_time DESC",
           countQuery = "SELECT COUNT(i.id) FROM inventory i " +
                        "JOIN materials m ON m.id = i.material_id " +
                        "WHERE (:keyword IS NULL OR m.material_name ILIKE '%' || :keyword || '%' " +
                        "    OR m.material_code ILIKE '%' || :keyword || '%') " +
                        "AND (:status IS NULL OR i.status = :status)",
           nativeQuery = true)
    Page<Inventory> findByConditions(@Param("keyword") String keyword,
                                     @Param("status") Integer status,
                                     Pageable pageable);

    @Query("SELECT i FROM Inventory i WHERE i.expiryDate <= :alertDate AND i.status = 1 AND i.quantity > 0")
    List<Inventory> findExpiringInventory(@Param("alertDate") LocalDate alertDate);

    long countByStatus(Integer status);

    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM Inventory i WHERE i.materialId = :materialId AND i.status = 1")
    Integer sumQuantityByMaterialId(@Param("materialId") Long materialId);

    @Query("SELECT COUNT(DISTINCT i.materialId) FROM Inventory i WHERE i.quantity < " +
           "(SELECT m.minStock FROM Material m WHERE m.id = i.materialId) AND i.status = 1")
    Long countLowStockMaterials();

    /** FEFO（先进先出按效期）查询可用批次，效期最近的排在最前 */
    @Query("SELECT i FROM Inventory i WHERE i.materialId = :materialId AND i.status = 1 AND i.quantity > 0 " +
           "ORDER BY i.expiryDate ASC NULLS LAST, i.id ASC")
    List<Inventory> findAvailableByMaterialIdFEFO(@Param("materialId") Long materialId);

    List<Inventory> findByMaterialIdAndBatchNumber(Long materialId, String batchNumber);

    /** 批量统计多种耗材的总在库数量（解决 N+1 问题） */
    @Query("SELECT i.materialId, COALESCE(SUM(i.quantity), 0) FROM Inventory i " +
           "WHERE i.materialId IN :materialIds AND i.status = 1 GROUP BY i.materialId")
    List<Object[]> sumQuantityGroupByMaterialIds(@Param("materialIds") List<Long> materialIds);

    /** 悲观锁查询单条库存（用于出库防超卖） */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.id = :id")
    Optional<Inventory> findByIdForUpdate(@Param("id") Long id);

    /** FEFO 查询可用批次并加写锁（用于申领发放防超卖） */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.materialId = :materialId AND i.status = 1 AND i.quantity > 0 " +
           "ORDER BY i.expiryDate ASC NULLS LAST, i.id ASC")
    List<Inventory> findAvailableByMaterialIdFEFOForUpdate(@Param("materialId") Long materialId);

    @Query("SELECT i FROM Inventory i WHERE i.materialId = :materialId AND i.status <> :status")
    List<Inventory> findByMaterialIdAndStatusNot(@Param("materialId") Long materialId, @Param("status") Integer status);

    @Query(value = "SELECT i.* FROM inventory i " +
                   "JOIN materials m ON m.id = i.material_id " +
                   "WHERE (:keyword IS NULL OR m.material_name ILIKE '%' || :keyword || '%' " +
                   "    OR m.material_code ILIKE '%' || :keyword || '%') " +
                   "AND (:inspectionStatus IS NULL OR i.inspection_status = :inspectionStatus) " +
                   "ORDER BY i.create_time DESC",
           countQuery = "SELECT COUNT(i.id) FROM inventory i " +
                        "JOIN materials m ON m.id = i.material_id " +
                        "WHERE (:keyword IS NULL OR m.material_name ILIKE '%' || :keyword || '%' " +
                        "    OR m.material_code ILIKE '%' || :keyword || '%') " +
                        "AND (:inspectionStatus IS NULL OR i.inspection_status = :inspectionStatus)",
           nativeQuery = true)
    Page<Inventory> findByInspectionConditions(@Param("keyword") String keyword,
                                               @Param("inspectionStatus") String inspectionStatus,
                                               Pageable pageable);

    @Query("SELECT i.supplierId, COUNT(i) as total, " +
           "SUM(CASE WHEN i.inspectionStatus = 'PASSED' THEN 1 ELSE 0 END) as passed " +
           "FROM Inventory i WHERE i.supplierId IN :supplierIds GROUP BY i.supplierId")
    List<Object[]> findQualityRateBySupplierIds(@Param("supplierIds") List<Long> supplierIds);
}
