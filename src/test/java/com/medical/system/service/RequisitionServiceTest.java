package com.medical.system.service;

import com.medical.system.dto.request.ApprovalRequest;
import com.medical.system.dto.request.CreateRequisitionRequest;
import com.medical.system.dto.response.RequisitionResponse;
import com.medical.system.entity.*;
import com.medical.system.exception.BusinessException;
import com.medical.system.repository.*;
import com.medical.system.security.CustomUserDetails;
import com.medical.system.service.impl.RequisitionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequisitionServiceTest {

    @Mock private RequisitionRepository requisitionRepository;
    @Mock private RequisitionItemRepository requisitionItemRepository;
    @Mock private ApprovalRecordRepository approvalRecordRepository;
    @Mock private MaterialRepository materialRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private InventoryTransactionRepository inventoryTransactionRepository;

    @InjectMocks
    private RequisitionServiceImpl requisitionService;

    private Requisition sampleRequisition;

    @BeforeEach
    void setUp() {
        sampleRequisition = new Requisition();
        sampleRequisition.setId(1L);
        sampleRequisition.setRequisitionNo("REQ202403100001");
        sampleRequisition.setDeptId(1L);
        sampleRequisition.setStatus("DRAFT");
        sampleRequisition.setCreatedBy(1L);
        sampleRequisition.setCreateTime(LocalDateTime.now());
        sampleRequisition.setRequisitionDate(LocalDateTime.now());
    }

    @Test
    void testCreateRequisition_success() {
        CreateRequisitionRequest request = new CreateRequisitionRequest();
        request.setDeptId(1L);
        request.setRequiredDate(LocalDate.now().plusDays(3));
        request.setRemark("测试申领");

        CreateRequisitionRequest.ItemRequest itemReq = new CreateRequisitionRequest.ItemRequest();
        itemReq.setMaterialId(1L);
        itemReq.setQuantity(10);
        request.setItems(List.of(itemReq));

        when(requisitionRepository.save(any(Requisition.class))).thenAnswer(invocation -> {
            Requisition req = invocation.getArgument(0);
            req.setId(1L);
            req.setCreateTime(LocalDateTime.now());
            return req;
        });
        when(requisitionItemRepository.findByRequisitionId(anyLong())).thenReturn(Collections.emptyList());
        when(approvalRecordRepository.findByRequisitionIdOrderByApprovalTimeAsc(anyLong())).thenReturn(Collections.emptyList());
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(createDept()));

        RequisitionResponse response = requisitionService.createRequisition(request, 1L);

        assertNotNull(response);
        assertEquals("DRAFT", response.getStatus());
        assertEquals(1L, response.getDeptId());
        verify(requisitionRepository).save(any(Requisition.class));
        verify(requisitionItemRepository).save(any(RequisitionItem.class));
    }

    @Test
    void testSubmitRequisition_success() {
        sampleRequisition.setStatus("DRAFT");

        try (MockedStatic<com.medical.system.security.SecurityUtils> secUtils = mockStatic(com.medical.system.security.SecurityUtils.class)) {
            secUtils.when(com.medical.system.security.SecurityUtils::canAccessAllDepts).thenReturn(true);

            when(requisitionRepository.findById(1L)).thenReturn(Optional.of(sampleRequisition));
            when(requisitionRepository.save(any(Requisition.class))).thenAnswer(inv -> inv.getArgument(0));
            when(requisitionItemRepository.findByRequisitionId(anyLong())).thenReturn(Collections.emptyList());
            when(approvalRecordRepository.findByRequisitionIdOrderByApprovalTimeAsc(anyLong())).thenReturn(Collections.emptyList());
            when(departmentRepository.findById(anyLong())).thenReturn(Optional.of(createDept()));

            RequisitionResponse response = requisitionService.submitRequisition(1L);

            assertEquals("PENDING", response.getStatus());
        }
    }

    @Test
    void testApproveRequisition_success() {
        sampleRequisition.setStatus("PENDING");
        ApprovalRequest approvalRequest = new ApprovalRequest();
        approvalRequest.setRemark("同意");

        try (MockedStatic<com.medical.system.security.SecurityUtils> secUtils = mockStatic(com.medical.system.security.SecurityUtils.class)) {
            secUtils.when(com.medical.system.security.SecurityUtils::canAccessAllDepts).thenReturn(true);

            when(requisitionRepository.findById(1L)).thenReturn(Optional.of(sampleRequisition));
            when(requisitionRepository.save(any(Requisition.class))).thenAnswer(inv -> inv.getArgument(0));
            when(approvalRecordRepository.save(any(ApprovalRecord.class))).thenAnswer(inv -> inv.getArgument(0));
            when(requisitionItemRepository.findByRequisitionId(anyLong())).thenReturn(Collections.emptyList());
            when(approvalRecordRepository.findByRequisitionIdOrderByApprovalTimeAsc(anyLong())).thenReturn(Collections.emptyList());
            when(departmentRepository.findById(anyLong())).thenReturn(Optional.of(createDept()));

            RequisitionResponse response = requisitionService.approveRequisition(1L, 2L, approvalRequest);

            assertEquals("APPROVED", response.getStatus());
            verify(approvalRecordRepository).save(any(ApprovalRecord.class));
        }
    }

    @Test
    void testDispatchRequisition_insufficientStock() {
        sampleRequisition.setStatus("APPROVED");

        RequisitionItem item = new RequisitionItem();
        item.setId(1L);
        item.setRequisitionId(1L);
        item.setMaterialId(1L);
        item.setQuantity(100);

        when(requisitionRepository.findById(1L)).thenReturn(Optional.of(sampleRequisition));
        when(requisitionItemRepository.findByRequisitionId(1L)).thenReturn(List.of(item));
        when(inventoryRepository.findAvailableByMaterialIdFEFO(1L)).thenReturn(Collections.emptyList());

        Material mat = new Material();
        mat.setId(1L);
        mat.setMaterialName("测试耗材");
        when(materialRepository.findById(1L)).thenReturn(Optional.of(mat));

        assertThrows(BusinessException.class, () -> requisitionService.dispatchRequisition(1L));
    }

    private Department createDept() {
        Department dept = new Department();
        dept.setId(1L);
        dept.setDeptName("测试科室");
        return dept;
    }
}
