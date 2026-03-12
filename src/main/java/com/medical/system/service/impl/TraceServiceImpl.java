package com.medical.system.service.impl;

import com.medical.system.entity.*;
import com.medical.system.exception.BusinessException;
import com.medical.system.repository.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TraceServiceImpl {

    private final SurgeryRecordRepository surgeryRepo;
    private final SurgeryMaterialBindingRepository bindingRepo;
    private final MaterialUdiRepository udiRepo;
    private final MaterialRepository materialRepo;
    private final InventoryRepository inventoryRepo;
    private final InventoryTransactionRepository transactionRepo;
    private final DepartmentRepository deptRepo;

    public PatientTraceVO traceByPatient(String patientId) {
        List<SurgeryRecord> surgeries = surgeryRepo.findByPatientId(patientId);
        if (surgeries.isEmpty()) throw new BusinessException("未找到患者记录：" + patientId);

        PatientTraceVO vo = new PatientTraceVO();
        vo.setPatientId(patientId);
        vo.setPatientName(surgeries.get(0).getPatientName());
        vo.setSurgeries(surgeries.stream().map(s -> {
            SurgeryTraceVO sv = new SurgeryTraceVO();
            sv.setSurgeryId(s.getId());
            sv.setSurgeryNo(s.getSurgeryNo());
            sv.setSurgeryDate(s.getSurgeryDate());
            sv.setSurgeryType(s.getSurgeryType());
            sv.setDoctorName(s.getDoctorName());
            if (s.getDeptId() != null) {
                deptRepo.findById(s.getDeptId()).ifPresent(d -> sv.setDeptName(d.getDeptName()));
            }
            List<SurgeryMaterialBinding> bindings = bindingRepo.findBySurgeryId(s.getId());
            sv.setMaterials(bindings.stream().map(b -> {
                MaterialTraceItem item = new MaterialTraceItem();
                item.setMaterialId(b.getMaterialId());
                item.setQuantity(b.getQuantity());
                item.setUseDate(b.getUseDate());
                item.setRemark(b.getRemark());
                materialRepo.findById(b.getMaterialId()).ifPresent(m -> {
                    item.setMaterialName(m.getMaterialName());
                    item.setMaterialCode(m.getMaterialCode());
                    item.setSpecification(m.getSpecification());
                });
                if (b.getUdiId() != null) {
                    udiRepo.findById(b.getUdiId()).ifPresent(u -> item.setUdiCode(u.getUdiCode()));
                }
                return item;
            }).collect(Collectors.toList()));
            return sv;
        }).collect(Collectors.toList()));
        return vo;
    }

    public MaterialTraceVO traceByMaterial(Long materialId) {
        Material material = materialRepo.findById(materialId)
                .orElseThrow(() -> new BusinessException("耗材不存在"));
        MaterialTraceVO vo = new MaterialTraceVO();
        vo.setMaterialId(materialId);
        vo.setMaterialName(material.getMaterialName());
        vo.setMaterialCode(material.getMaterialCode());

        // 手术绑定记录
        List<SurgeryMaterialBinding> bindings = bindingRepo.findByMaterialId(materialId);
        vo.setSurgeryUsages(bindings.stream().map(b -> {
            SurgeryUsageVO uvo = new SurgeryUsageVO();
            uvo.setBindingId(b.getId());
            uvo.setSurgeryId(b.getSurgeryId());
            uvo.setQuantity(b.getQuantity());
            uvo.setUseDate(b.getUseDate());
            surgeryRepo.findById(b.getSurgeryId()).ifPresent(s -> {
                uvo.setSurgeryNo(s.getSurgeryNo());
                uvo.setPatientName(s.getPatientName());
                uvo.setSurgeryDate(s.getSurgeryDate());
            });
            return uvo;
        }).collect(Collectors.toList()));

        // 出库记录
        List<InventoryTransaction> txs = transactionRepo.findByMaterialIdAndTransactionType(materialId, "OUTBOUND");
        vo.setOutboundRecords(txs.stream().limit(20).map(t -> {
            OutboundRecordVO ov = new OutboundRecordVO();
            ov.setTxId(t.getId());
            ov.setQuantity(t.getQuantity());
            ov.setBatchNumber(t.getBatchNumber());
            ov.setCreateTime(t.getCreateTime());
            ov.setRemark(t.getRemark());
            if (t.getDeptId() != null) {
                deptRepo.findById(t.getDeptId()).ifPresent(d -> ov.setDeptName(d.getDeptName()));
            }
            return ov;
        }).collect(Collectors.toList()));

        return vo;
    }

    public UdiTraceVO traceByUdi(String udiCode) {
        MaterialUdi udi = udiRepo.findByUdiCode(udiCode)
                .orElseThrow(() -> new BusinessException("UDI不存在：" + udiCode));
        UdiTraceVO vo = new UdiTraceVO();
        vo.setUdiId(udi.getId());
        vo.setUdiCode(udi.getUdiCode());
        vo.setStatus(udi.getStatus());
        vo.setBatchNumber(udi.getBatchNumber());
        vo.setManufactureDate(udi.getManufactureDate());
        vo.setExpiryDate(udi.getExpiryDate());
        materialRepo.findById(udi.getMaterialId()).ifPresent(m -> {
            vo.setMaterialName(m.getMaterialName());
            vo.setMaterialCode(m.getMaterialCode());
        });

        // 手术使用记录
        List<SurgeryMaterialBinding> bindings = bindingRepo.findByUdiId(udi.getId());
        if (!bindings.isEmpty()) {
            SurgeryMaterialBinding b = bindings.get(0);
            surgeryRepo.findById(b.getSurgeryId()).ifPresent(s -> {
                vo.setUsedInSurgeryNo(s.getSurgeryNo());
                vo.setUsedPatientName(s.getPatientName());
                vo.setUsedDate(s.getSurgeryDate());
            });
        }
        return vo;
    }

    @Data public static class PatientTraceVO {
        private String patientId; private String patientName;
        private List<SurgeryTraceVO> surgeries;
    }
    @Data public static class SurgeryTraceVO {
        private Long surgeryId; private String surgeryNo; private LocalDateTime surgeryDate;
        private String surgeryType; private String doctorName; private String deptName;
        private List<MaterialTraceItem> materials;
    }
    @Data public static class MaterialTraceItem {
        private Long materialId; private String materialName; private String materialCode;
        private String specification; private String udiCode; private Integer quantity;
        private LocalDateTime useDate; private String remark;
    }
    @Data public static class MaterialTraceVO {
        private Long materialId; private String materialName; private String materialCode;
        private List<SurgeryUsageVO> surgeryUsages; private List<OutboundRecordVO> outboundRecords;
    }
    @Data public static class SurgeryUsageVO {
        private Long bindingId; private Long surgeryId; private String surgeryNo;
        private String patientName; private LocalDateTime surgeryDate; private Integer quantity; private LocalDateTime useDate;
    }
    @Data public static class OutboundRecordVO {
        private Long txId; private Integer quantity; private String batchNumber;
        private String deptName; private LocalDateTime createTime; private String remark;
    }
    @Data public static class UdiTraceVO {
        private Long udiId; private String udiCode; private String materialName; private String materialCode;
        private String batchNumber; private java.time.LocalDate manufactureDate; private java.time.LocalDate expiryDate;
        private String status; private String usedInSurgeryNo; private String usedPatientName; private LocalDateTime usedDate;
    }
}
