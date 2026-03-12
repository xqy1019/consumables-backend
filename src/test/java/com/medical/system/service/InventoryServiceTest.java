package com.medical.system.service;

import com.medical.system.dto.request.InboundRequest;
import com.medical.system.dto.request.OutboundRequest;
import com.medical.system.dto.response.InventoryResponse;
import com.medical.system.entity.Inventory;
import com.medical.system.entity.Material;
import com.medical.system.exception.BusinessException;
import com.medical.system.repository.*;
import com.medical.system.service.impl.InventoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock private InventoryRepository inventoryRepository;
    @Mock private MaterialRepository materialRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private UserRepository userRepository;
    @Mock private InventoryTransactionRepository transactionRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    @Test
    void testInbound_success() {
        InboundRequest request = new InboundRequest();
        request.setMaterialId(1L);
        request.setQuantity(100);
        request.setBatchNumber("BATCH001");
        request.setLocation("A-1-1");
        request.setExpiryDate(LocalDate.now().plusYears(1));

        Material material = new Material();
        material.setId(1L);
        material.setMaterialName("测试耗材");
        material.setMaterialCode("MAT001");

        when(materialRepository.existsById(1L)).thenReturn(true);
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> {
            Inventory saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(materialRepository.findById(1L)).thenReturn(Optional.of(material));
        when(inventoryRepository.sumQuantityByMaterialId(1L)).thenReturn(100);

        InventoryResponse response = inventoryService.inbound(request);

        assertNotNull(response);
        assertEquals(100, response.getQuantity());
        assertEquals("BATCH001", response.getBatchNumber());
        verify(inventoryRepository).save(any(Inventory.class));
        verify(transactionRepository).save(any());
    }

    @Test
    void testOutbound_success() {
        Inventory inventory = new Inventory();
        inventory.setId(1L);
        inventory.setMaterialId(1L);
        inventory.setQuantity(50);
        inventory.setBatchNumber("BATCH001");
        inventory.setStatus(1);

        OutboundRequest request = new OutboundRequest();
        request.setInventoryId(1L);
        request.setQuantity(20);
        request.setDeptId(1L);
        request.setRemark("出库测试");

        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        inventoryService.outbound(request);

        assertEquals(30, inventory.getQuantity());
        verify(inventoryRepository).save(inventory);
        verify(transactionRepository).save(any());
    }

    @Test
    void testOutbound_insufficientStock() {
        Inventory inventory = new Inventory();
        inventory.setId(1L);
        inventory.setMaterialId(1L);
        inventory.setQuantity(10);
        inventory.setBatchNumber("BATCH001");

        OutboundRequest request = new OutboundRequest();
        request.setInventoryId(1L);
        request.setQuantity(20);

        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));

        assertThrows(BusinessException.class, () -> inventoryService.outbound(request));
    }
}
