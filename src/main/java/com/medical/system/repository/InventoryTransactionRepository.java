package com.medical.system.repository;

import com.medical.system.entity.InventoryTransaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    @Query("SELECT t FROM InventoryTransaction t WHERE t.createTime >= :startTime ORDER BY t.createTime DESC")
    List<InventoryTransaction> findRecentTransactions(@Param("startTime") LocalDateTime startTime,
                                                       Pageable pageable);

    List<InventoryTransaction> findByMaterialIdAndTransactionType(Long materialId, String transactionType);

    @Query("SELECT t FROM InventoryTransaction t WHERE t.materialId = :materialId ORDER BY t.createTime DESC")
    List<InventoryTransaction> findByMaterialId(@Param("materialId") Long materialId);

    @Query("SELECT t FROM InventoryTransaction t WHERE t.deptId = :deptId " +
           "AND t.transactionType = 'OUTBOUND' AND t.createTime >= :since")
    List<InventoryTransaction> findOutboundByDeptSince(@Param("deptId") Long deptId,
                                                       @Param("since") LocalDateTime since);

    @Query(value = "SELECT to_char(create_time, 'YYYY-MM-DD') AS day, " +
                   "transaction_type, SUM(quantity) AS total " +
                   "FROM inventory_transactions " +
                   "WHERE create_time >= :startTime " +
                   "GROUP BY to_char(create_time, 'YYYY-MM-DD'), transaction_type " +
                   "ORDER BY day",
           nativeQuery = true)
    List<Object[]> findWeeklyTrend(@Param("startTime") LocalDateTime startTime);

    /** 近N月内，指定科室各耗材的出库总量。返回 [materialId, totalQty] */
    @Query("SELECT t.materialId, SUM(t.quantity) FROM InventoryTransaction t " +
           "WHERE t.deptId = :deptId AND t.transactionType = 'OUTBOUND' AND t.createTime >= :since " +
           "GROUP BY t.materialId")
    List<Object[]> sumOutboundByMaterialForDept(@Param("deptId") Long deptId,
                                                 @Param("since") LocalDateTime since);

    /** 近N月内，所有科室各耗材的出库总量。返回 [materialId, totalQty] */
    @Query("SELECT t.materialId, SUM(t.quantity) FROM InventoryTransaction t " +
           "WHERE t.transactionType = 'OUTBOUND' AND t.createTime >= :since " +
           "GROUP BY t.materialId")
    List<Object[]> findTotalOutboundByMaterialSince(@Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(SUM(t.quantity), 0) FROM InventoryTransaction t " +
           "WHERE t.materialId = :materialId AND t.transactionType = 'OUTBOUND' " +
           "AND t.createTime >= :startTime AND t.createTime < :endTime")
    Integer sumActualOutbound(@Param("materialId") Long materialId,
                              @Param("startTime") LocalDateTime startTime,
                              @Param("endTime") LocalDateTime endTime);

    /** 按耗材+科室+日期分组的日出库量，用于异常检测。返回 [materialId, deptId, day, qty] */
    @Query(value = "SELECT material_id, dept_id, " +
                   "to_char(create_time, 'YYYY-MM-DD') AS day, " +
                   "SUM(quantity) AS qty " +
                   "FROM inventory_transactions " +
                   "WHERE transaction_type = 'OUTBOUND' AND create_time >= :since " +
                   "GROUP BY material_id, dept_id, to_char(create_time, 'YYYY-MM-DD') " +
                   "ORDER BY material_id, dept_id, day",
           nativeQuery = true)
    List<Object[]> findDailyOutboundGrouped(@Param("since") LocalDateTime since);
}
