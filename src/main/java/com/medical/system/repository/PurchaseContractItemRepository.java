package com.medical.system.repository;

import com.medical.system.entity.PurchaseContractItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PurchaseContractItemRepository extends JpaRepository<PurchaseContractItem, Long> {
    List<PurchaseContractItem> findByContractId(Long contractId);

    @Query("SELECT pc.supplierId, AVG(pci.unitPrice), COUNT(pci) FROM PurchaseContractItem pci " +
           "JOIN PurchaseContract pc ON pc.id = pci.contractId " +
           "WHERE pci.materialId = :materialId AND pc.status IN ('ACTIVE','COMPLETED') " +
           "GROUP BY pc.supplierId")
    List<Object[]> findSupplierPriceStats(@Param("materialId") Long materialId);
}
