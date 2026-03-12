package com.medical.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.system.common.PageResult;
import com.medical.system.dto.request.CreateRequisitionRequest;
import com.medical.system.dto.response.RequisitionResponse;
import com.medical.system.security.JwtTokenProvider;
import com.medical.system.service.OperationLogService;
import com.medical.system.service.impl.RequisitionServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RequisitionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RequisitionServiceImpl requisitionService;

    @MockBean
    private OperationLogService operationLogService;

    @Test
    void testCreateRequisition_unauthorized() throws Exception {
        CreateRequisitionRequest request = new CreateRequisitionRequest();
        request.setDeptId(1L);
        request.setRequiredDate(LocalDate.now().plusDays(3));

        CreateRequisitionRequest.ItemRequest itemReq = new CreateRequisitionRequest.ItemRequest();
        itemReq.setMaterialId(1L);
        itemReq.setQuantity(10);
        request.setItems(List.of(itemReq));

        mockMvc.perform(post("/api/v1/requisitions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"menu:requisition"})
    void testCreateRequisition_success() throws Exception {
        CreateRequisitionRequest request = new CreateRequisitionRequest();
        request.setDeptId(1L);
        request.setRequiredDate(LocalDate.now().plusDays(3));

        CreateRequisitionRequest.ItemRequest itemReq = new CreateRequisitionRequest.ItemRequest();
        itemReq.setMaterialId(1L);
        itemReq.setQuantity(10);
        request.setItems(List.of(itemReq));

        RequisitionResponse response = new RequisitionResponse();
        response.setId(1L);
        response.setRequisitionNo("REQ202403100001");
        response.setStatus("DRAFT");
        response.setDeptId(1L);

        when(requisitionService.createRequisition(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/requisitions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }
}
